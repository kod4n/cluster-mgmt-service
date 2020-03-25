package io.cratekube.clustermgmt.service

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.helper.StringHelpers
import groovy.util.logging.Slf4j
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ClusterApi
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.model.Cluster
import io.cratekube.clustermgmt.model.ClusterNode
import io.cratekube.clustermgmt.model.Kubeconfig
import io.cratekube.clustermgmt.model.hbs.RkeClusterConfig
import io.cratekube.clustermgmt.model.hbs.RkeClusterNode
import io.cratekube.clustermgmt.modules.annotation.ClusterCache
import io.cratekube.clustermgmt.modules.annotation.ClusterExecutor
import io.cratekube.clustermgmt.modules.annotation.ClusterProcessExecutor
import org.apache.commons.vfs2.FileSystemManager
import org.yaml.snakeyaml.Yaml

import javax.inject.Inject
import java.util.concurrent.Executor

import static io.cratekube.clustermgmt.model.Status.COMPLETED
import static io.cratekube.clustermgmt.model.Status.FAILED
import static io.cratekube.clustermgmt.model.Status.IN_PROGRESS
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.not
import static org.hamcrest.core.Every.everyItem
import static org.hamcrest.core.IsNull.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

/**
 * Default implementation for the {@link ClusterApi} interface.  Provides access to operation on clusters.
 *
 * @see ClusterApi
 */
@Slf4j
class ClusterService implements ClusterApi {
  FileSystemManager fs
  ProcessExecutor rke
  ManagedResourcesApi resources
  AppConfig config

  // for async operations
  Executor executor
  /** map of envName/clusterName to cluster details */
  Map<String, Cluster> clusterCache

  @Inject
  ClusterService(FileSystemManager fs, @ClusterProcessExecutor ProcessExecutor rke, ManagedResourcesApi resources,
                 AppConfig config, @ClusterExecutor Executor executor, @ClusterCache Map<String, Cluster> clusterCache) {
    this.fs = require fs, notNullValue()
    this.rke = require rke, notNullValue()
    this.resources = require resources, notNullValue()
    this.config = require config, notNullValue()
    this.executor = require executor, notNullValue()
    this.clusterCache = require clusterCache, notNullValue()
  }

  @Override
  @SuppressWarnings('AbcMetric')
  void bootstrapCluster(String envName, String clusterName, List<String> hostnames)
    throws AlreadyExistsException, InProgressException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require hostnames, allOf(notNullValue(), not(empty()), everyItem(notEmptyString()))

    // call getCluster to look for an existing cluster status object in the in-memory LRU cache and throw InProgressException if it is still processing
    // and throw AlreadyExistsException if the desired cluster already exists
    try {
      def cluster = getCluster(envName, clusterName)
      if (cluster.nodes.status.any { it == IN_PROGRESS }) {
        throw new InProgressException("Cluster operation in progress for cluster [${clusterName}].")
      }
      if (cluster.nodes.status.any { it == COMPLETED }) {
        throw new AlreadyExistsException("Cluster ${clusterName} already exists.")
      }
    } catch (NotFoundException ex) {
      log.debug "${ex.message}. Attempting bootstrap."
    }

    // put initial cluster status in the cluster queue. queueKey = "${envName}/${name}"
    String cacheKey = "${envName}/${clusterName}"
    clusterCache[cacheKey] = new Cluster(
      envName: envName,
      name: clusterName,
      nodes: hostnames.collect { new ClusterNode(hostname:  it, status:  IN_PROGRESS)}
    )

    // background execution through executor
    executor.execute {
      // generate list of nodes for the rke-cluster-config template
      // generate list of nodes for the template
      def nodes = hostnames.withIndex().collect { hostname, index ->
        new RkeClusterNode(
          address: hostname,
          user: config.rke.nodeUser,
          roles: index == 0 ? ['controlplane', 'etcd'] : ['worker']
        )
      }

      // create cluster config by applying template in the resources directory and update cached config
      // create cluster config using template in the resources directory
      def handlebars = new Handlebars().with { registerHelpers(StringHelpers) }
      def template = handlebars.compile('rke-cluster-config')
      def rkeClusterConfig = new RkeClusterConfig(sshKeyPath: config.rke.sshKeyPath, rkeClusterNodes: nodes)

      // config file should be saved
      String clusterYmlPath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/cluster.yml"
      def clusterYml = fs.resolveFile(clusterYmlPath)
      clusterYml.content.outputStream.withPrintWriter {
        def clusterConfig = template.apply(rkeClusterConfig)
        it.write clusterConfig
        clusterCache[cacheKey].config = clusterConfig
      }

      // run `rke up` using the generated config file in the cluster name directory and update the node statuses
      def proc = rke.exec('up', '--config', "${clusterYmlPath}")
      proc.waitForProcessOutput(System.out, System.err)
      if (proc.exitValue() != 0) {
        clusterCache[cacheKey].nodes = hostnames.collect { new ClusterNode(hostname:  it, status:  FAILED)}
        return
      }

      // deploy additional k8s resources required for configuring cluster using the ManagedResourcesApi
      resources.bootstrapManagedResources(envName, clusterName)
      clusterCache[cacheKey].nodes = hostnames.collect { new ClusterNode(hostname:  it, status:  COMPLETED)}
    }
  }

  @Override
  void destroyCluster(String envName, String clusterName)
    throws InProgressException, NotFoundException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    // call getCluster to look for an existing cluster object in the in-memory LRU cache and throw InProgressException if it is still processing
    // and throw NotFoundException if the desired cluster does not exist
    def cluster = getCluster(envName, clusterName)
    if (cluster.nodes.status.any { it == IN_PROGRESS}) {
      throw new InProgressException("Cluster operation in progress for cluster [${clusterName}].")
    }
    // config file should be saved
    // /environment/{envName}/cluster/{clustName}/cluster.yml
    String clusterPath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}"
    String clusterFilePath = "${clusterPath}/cluster.yml"

    // run `rke delete` using the  config file in the cluster clustName directory and remove item from cluster queue if it exists
    executor.execute {
      String cacheKey = "${envName}/${clusterName}"
      // set to in progress
      clusterCache[cacheKey] = new Cluster(
        envName:  envName,
        name:  clusterName,
        config: cluster.config,
        nodes: cluster.nodes.hostname.collect { new ClusterNode(hostname: it, status: IN_PROGRESS) }
      )
      def proc = rke.exec('remove', '--force --config', "${clusterFilePath}")
      proc.waitForProcessOutput(System.out, System.err)
      if (proc.exitValue() != 0) {
        clusterCache[cacheKey].nodes = cluster.nodes.hostname.collect { new ClusterNode(hostname: it, status: FAILED) }
        return
      }
      clusterCache.remove(cacheKey)
      fs.resolveFile(clusterPath).deleteAll()
    }
  }

  @Override
  Cluster getCluster(String envName, String clusterName)
    throws NotFoundException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    // look for an existing cluster status object in the in-memory LRU cache and return it if it exists
    String cacheKey = "${envName}/${clusterName}"
    if (clusterCache[cacheKey]) {
      return clusterCache[cacheKey]
    }

    // if no still processing cache value exists search for a config file created by rke at DEFAULT_CONFIG_LOCATION/environment/{envName}/cluster/{cluster}/kube_config_cluster.yml
    // build cluster object if file is found.
    String kubeFilePath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/kube_config_cluster.yml"
    def kubeYml = fs.resolveFile(kubeFilePath)
    if (kubeYml.exists()) {
      def clusterYmlPath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/cluster.yml"
      def clusterYml = fs.resolveFile(clusterYmlPath)
      def clusterYmlText = clusterYml.content.inputStream.text
      def clusterObj = new Yaml().load(clusterYmlText)
      return new Cluster(
        envName:  envName,
        name: clusterName,
        config: clusterYmlText,
        nodes: clusterObj['nodes'].collect { new ClusterNode(hostname:  it['address'], status:  COMPLETED) }
      )
    }

    // otherwise throw NotFoundException
    throw new NotFoundException("Cluster [${clusterName}] not found.")
  }

  @Override
  Kubeconfig getCustomerKubeconfig(String envName, String clusterName)
    throws NotFoundException, InProgressException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    // look for an existing cluster object in the in-memory LRU cache and throw InProgressException if it is still processing
    def cluster = getCluster(envName, clusterName)
    if (cluster.nodes.status.any { it == IN_PROGRESS}) {
      throw new InProgressException("Cluster operation in progress for cluster [${clusterName}].")
    }

    // if no still processing cache value exists search for a config file created by rke at ${config.configLocation}/environment/{envName}/cluster/{cluster}/customer_kube_config.yml
    def customerKubeconfigPath = "${config.configLocation}/environment/${envName}/cluster/${clusterName}/customer_kube_config.yml"
    def clusterYml = fs.resolveFile(customerKubeconfigPath)
    if (clusterYml.exists()) {
      def customerKubeconfigText = clusterYml.content.inputStream.text
      return new Kubeconfig(customerKubeconfigText)
    }

    // otherwise throw NotFoundException
    throw new NotFoundException("Customer kubeconfig for cluster [${clusterName}] not found.")
  }
}
