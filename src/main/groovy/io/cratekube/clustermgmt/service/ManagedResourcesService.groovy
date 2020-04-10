package io.cratekube.clustermgmt.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.helper.StringHelpers
import groovy.util.logging.Slf4j
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.model.ManagedResource
import io.cratekube.clustermgmt.model.hbs.ClusterRoleBinding
import io.cratekube.clustermgmt.model.hbs.CustomerKubeconfig
import io.cratekube.clustermgmt.model.hbs.Namespace
import io.cratekube.clustermgmt.model.hbs.ServiceAccount
import io.cratekube.clustermgmt.modules.annotation.ManagedResourceCache
import io.cratekube.clustermgmt.modules.annotation.ManagedResourceExecutor
import io.cratekube.clustermgmt.modules.annotation.ManagedResourceProcessExecutor
import org.apache.commons.vfs2.FileSystemManager
import org.apache.commons.vfs2.Selectors

import javax.inject.Inject
import java.util.concurrent.Executor

import static io.cratekube.clustermgmt.model.Status.IN_PROGRESS
import static io.cratekube.clustermgmt.model.Status.FAILED
import static io.cratekube.clustermgmt.model.Status.COMPLETED
import static org.hamcrest.core.IsNull.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

/**
 * Default implementation fo the {@link ManagedResourcesApi} interface.
 *
 * @see ManagedResourcesApi
 */
@Slf4j
class ManagedResourcesService implements ManagedResourcesApi {
  public static final String PERIOD = '.'
  FileSystemManager fs
  ProcessExecutor kubectl
  AppConfig config

  // for async operations
  Executor executor
  /** map of envName/clusterName to their managed resources. */
  Map<String, List<ManagedResource>> managedResourceCache

  @Inject
  ManagedResourcesService(FileSystemManager fs, @ManagedResourceProcessExecutor ProcessExecutor kubectl, AppConfig config,
                          @ManagedResourceExecutor Executor executor, @ManagedResourceCache Map<String, List<ManagedResource>> managedResourceCache) {
    this.fs = require fs, notNullValue()
    this.kubectl = require kubectl, notNullValue()
    this.config = require config, notNullValue()
    this.executor = require executor, notNullValue()
    this.managedResourceCache = require managedResourceCache, notNullValue()
  }

  @Override
  @SuppressWarnings('AbcMetric')
  void bootstrapManagedResources(String envName, String clusterName)
    throws NotFoundException, AlreadyExistsException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    log.debug 'Bootstrapping managed resources for environment [{}] cluster [{}]', envName, clusterName
    // throw exceptions if no cluster or if managed resources exist
    def managedResources = getManagedResources(envName, clusterName)
    if (!managedResources.empty && managedResources.every {it.status == COMPLETED}) {
      throw new AlreadyExistsException("Bootstrap resources already exist for cluster [${clusterName}].")
    }

    // set cache to empty list
    String cacheKey = "${envName}/${clusterName}"
    managedResourceCache[cacheKey] = []

    // constants for paths
    String clusterPath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}"
    String kubeFilePath = "${clusterPath}/kube_config_cluster.yml"
    String cratekubeNamespaceName = 'cratekube-namespace-namespace'
    String cratekubeNamespacePath = "${clusterPath}/resources/${cratekubeNamespaceName}.yml"
    String serviceaccountName = 'namespace-admin-serviceaccount'
    String serviceaccountPath = "${clusterPath}/resources/${serviceaccountName}.yml"
    String clusterRoleBindingName = 'cluster-admin-clusterrolebinding'
    String clusterRoleBindingPath = "${clusterPath}/resources/${clusterRoleBindingName}.yml"

    def handlebars = new Handlebars().with { registerHelpers(StringHelpers) }
    String cratekubeNamespace =  'cratekube-namespace'
    String namespaceAdmin =  'namespace-admin'
    // construct maps for apply loop
    [
      [name: cratekubeNamespaceName, templateName: 'namespace.yaml', ymlPath: cratekubeNamespacePath, configObject: new  Namespace(cratekubeNamespace)],
      [name: serviceaccountName, templateName: 'rbac/serviceaccounts/service-account.yaml', ymlPath: serviceaccountPath, configObject: new  ServiceAccount(namespaceAdmin, cratekubeNamespace)],
      [name: clusterRoleBindingName, templateName: 'rbac/clusterrolebindings/serviceaccount-cluster-admin.yaml', ymlPath: clusterRoleBindingPath, configObject: new  ClusterRoleBinding('cluster-admin:namespace-admin', namespaceAdmin, cratekubeNamespace)],
    ].each { bootstrapResource ->
      def template = handlebars.compile(bootstrapResource.templateName)
      def ymlPath = fs.resolveFile(bootstrapResource.ymlPath)
      def configObject = template.apply(bootstrapResource.configObject)
      ymlPath.content.outputStream.withPrintWriter {
        it.write configObject
      }
      // place resource in cache
      managedResourceCache[cacheKey] << new ManagedResource(
        name: bootstrapResource.name,
        config: configObject,
        status: IN_PROGRESS
      )
      def proc = kubectl.exec("--kubeconfig ${kubeFilePath} apply -f ${bootstrapResource.ymlPath}")
      proc.waitForProcessOutput(System.out, System.err)
      if (proc.exitValue() != 0) {
        managedResourceCache[cacheKey].find {it.name == bootstrapResource.name}.status = FAILED
        ymlPath.delete()
      } else {
        managedResourceCache[cacheKey].find {it.name == bootstrapResource.name}.status = COMPLETED
      }
    }

    // do not proceed if there are failures
    def failedResources = managedResourceCache[cacheKey].findAll {it.status == FAILED}
    if (failedResources) {
      log.debug 'Managed resources [{}] for environment [{}] cluster [{}] failed to bootstrap.', failedResources.name.join(','), envName, clusterName
      return
    }

    // generate the customer kubeconfig
    def mapper = new ObjectMapper()
    def (out, err) = [new StringBuffer(), new StringBuffer()]
    def proc = kubectl.exec("--kubeconfig ${kubeFilePath} get serviceAccounts ${namespaceAdmin} --namespace ${cratekubeNamespace} -o json")
    proc.waitForProcessOutput(out, err)
    log.debug out + err
    def serviceAccountJson = mapper.readValue(out.toString(), Map)
    def secretName = serviceAccountJson.secrets[0].name

    (out, err) = [new StringBuffer(), new StringBuffer()]
    proc = kubectl.exec("--kubeconfig ${kubeFilePath} get secrets ${secretName} --namespace ${cratekubeNamespace} -o json")
    proc.waitForProcessOutput(out, err)
    log.debug out + err
    def secretsJson = mapper.readValue(out.toString(), Map)
    def tokenValue = new String(secretsJson.data.token.decodeBase64())

    (out, err) = [new StringBuffer(), new StringBuffer()]
    proc = kubectl.exec("--kubeconfig ${kubeFilePath} config view --flatten --minify -o json")
    proc.waitForProcessOutput(out, err)
    log.debug out + err
    def configJson = mapper.readValue(out.toString(), Map)
    def certAuthData = configJson.clusters[0].cluster['certificate-authority-data']
    def server = configJson.clusters[0].cluster.server

    // write customer kubeconfig to file
    String customerKubeconfigPath = "${clusterPath}/customer_kube_config.yml"
    log.debug 'Writing customer kubeconfig to [{}]', customerKubeconfigPath
    def customerKubeconfigYml = fs.resolveFile(customerKubeconfigPath)
    def template = handlebars.compile('customer-kubeconfig.yaml')
    customerKubeconfigYml.content.outputStream.withPrintWriter {
      it.write template.apply(new CustomerKubeconfig(namespaceAdmin, tokenValue, certAuthData, server))
    }
  }

  @Override
  void deployManagedResource(String envName, String clusterName, ManagedResource resource)
    throws NotFoundException, InProgressException, AlreadyExistsException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require resource, notNullValue()

    // throw exceptions if no cluster or if bad managed resource states
    def managedResource = getManagedResource(envName, clusterName, resource.name)
    if (managedResource?.status == IN_PROGRESS) {
      throw new InProgressException("Managed Resource operation in progress for [${resource.name}].")
    }
    if (managedResource?.status == COMPLETED) {
      throw new AlreadyExistsException("Managed Resource [${resource.name}] already exists.")
    }

    // mark resource as in progress
    String cacheKey = "${envName}/${clusterName}"
    if (managedResourceCache[cacheKey]) {
      managedResourceCache[cacheKey].removeAll { it.name == resource.name }
    } else {
      managedResourceCache[cacheKey] = []
    }
    managedResourceCache[cacheKey] << new ManagedResource(
      name: resource.name,
      config: resource.config,
      status: IN_PROGRESS
    )

    // deploy the managed resource
    // background execution through executor
    executor.execute {
      String kubeFilePath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/kube_config_cluster.yml"
      String managedResourcePath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/resources/${resource.name}.yml"
      // write managed resource file
      def managedResourceYml = fs.resolveFile(managedResourcePath)
      managedResourceYml.content.outputStream.withPrintWriter {
        it.write resource.config
      }
      def proc = kubectl.exec("--kubeconfig ${kubeFilePath} apply -f ${managedResourcePath}")
      proc.waitForProcessOutput(System.out, System.err)
      if (proc.exitValue() != 0) {
        managedResourceCache[cacheKey].find { it.name == resource.name }.status = FAILED
        managedResourceYml.delete()
        return
      }
      managedResourceCache[cacheKey].find { it.name == resource.name }.status = COMPLETED
    }
  }

  @Override
  void removeManagedResource(String envName, String clusterName, String resourceName)
    throws NotFoundException, InProgressException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require resourceName, notEmptyString()

    // throw exceptions if no cluster or if bad managed resource states
    def managedResource = getManagedResource(envName, clusterName, resourceName)
    if (!managedResource) {
      throw new NotFoundException("Managed Resource [${resourceName}] not found.")
    }
    if (managedResource.status == IN_PROGRESS) {
      throw new InProgressException("Managed Resource operation in progress for [${resourceName}].")
    }

    // mark resource as in progress
    String cacheKey = "${envName}/${clusterName}"
    if (managedResourceCache[cacheKey]) {
      managedResourceCache[cacheKey].removeAll { it.name == resourceName }
    } else {
      managedResourceCache[cacheKey] = []
    }
    managedResourceCache[cacheKey] << new ManagedResource(
      name: managedResource.name,
      config: managedResource.config,
      status: IN_PROGRESS
    )

    // remove the managed resource
    // background execution through executor
    executor.execute {
      String kubeFilePath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/kube_config_cluster.yml"
      String managedResourcePath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/resources/${resourceName}.yml"
      def proc = kubectl.exec("--kubeconfig ${kubeFilePath} delete -f ${managedResourcePath}")
      proc.waitForProcessOutput(System.out, System.err)
      if (proc.exitValue() != 0) {
        managedResourceCache[cacheKey].find { it.name == resourceName }.status = FAILED
        return
      }

      // delete the file and null the cache
      managedResourceCache[cacheKey]?.removeAll { it.name == resourceName }
      fs.resolveFile(managedResourcePath).delete()
    }
  }

  @Override
  ManagedResource getManagedResource(String envName, String clusterName, String resourceName)
    throws NotFoundException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require resourceName, notEmptyString()

    // look for an existing object in the in-memory LRU cache and return it if it exists
    String cacheKey = "${envName}/${clusterName}"
    def maybeCachedResource = managedResourceCache[cacheKey]?.find { it.name == resourceName }
    if (maybeCachedResource) {
      return maybeCachedResource
    }

    // if no config file created by rke at ${config.configLocation}/environment/{envName}/cluster/{cluster}/kube_config_cluster.yml throw NotFoundException
    def customerKubeconfigPath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/kube_config_cluster.yml"
    def clusterYml = fs.resolveFile(customerKubeconfigPath)
    if (!clusterYml.exists()) {
      throw new NotFoundException("Cluster [${clusterName}] not found.")
    }

    // return all resources
    String managedResourcePath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/resources/${resourceName}.yml"
    def resourceYml = fs.resolveFile(managedResourcePath)
    if (resourceYml.exists()) {
      return new ManagedResource(
        name: resourceYml.name.baseName.take(resourceYml.name.baseName.lastIndexOf(PERIOD)),
        config: resourceYml.content.inputStream.text,
        status: COMPLETED
      )
    }

    // return null if no resource exist
    return null
  }

  @Override
  List<ManagedResource> getManagedResources(String envName, String clusterName)
    throws NotFoundException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    // if no config file created by rke at ${config.configLocation}/environment/{envName}/cluster/{cluster}/kube_config_cluster.yml throw NotFoundException
    def kubeconfigPath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/kube_config_cluster.yml"
    def clusterYml = fs.resolveFile(kubeconfigPath)
    if (!clusterYml.exists()) {
      throw new NotFoundException("Cluster [${clusterName}] not found.")
    }

    // return all resources
    String resourcesPath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/resources"
    def managedResourceFiles = fs.resolveFile(resourcesPath).findFiles(Selectors.SELECT_FILES)
    if (managedResourceFiles) {
      def managedResources = managedResourceFiles.collect {
        new ManagedResource(
          name: it.name.baseName.take(it.name.baseName.lastIndexOf(PERIOD)),
          config: it.content.inputStream.text,
          status: COMPLETED
        )
      }

      // look for an existing objects in the in-memory LRU cache and merge results
      String cacheKey = "${envName}/${clusterName}"
      List<ManagedResource> cachedNonCompleteResources = []
      if (managedResourceCache[cacheKey]) {
        cachedNonCompleteResources = managedResourceCache[cacheKey].findAll {it.status != COMPLETED}
      }
      managedResources.removeAll { fileResource -> cachedNonCompleteResources.any { fileResource.name == it.name } }
      return cachedNonCompleteResources + managedResources
    }
    // return empty list if no resources exist
    return []
  }
}
