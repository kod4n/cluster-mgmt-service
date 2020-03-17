package io.cratekube.clustermgmt.service

import com.google.common.util.concurrent.MoreExecutors
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.model.ManagedResource
import io.cratekube.clustermgmt.model.Status
import org.apache.commons.vfs2.FileContent
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.Executor

import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.not
import static org.hamcrest.Matchers.nullValue
import static org.hamcrest.Matchers.startsWith
import static spock.util.matcher.HamcrestSupport.expect
import static org.hamcrest.Matchers.notNullValue

class ManagedResourcesServiceSpec extends Specification {
  @Subject ManagedResourcesService subject
  FileSystemManager fs
  ProcessExecutor kubectl
  AppConfig config
  Executor executor
  Map<String, List<ManagedResource>> managedResourceCache

  def setup() {
    fs = Mock(FileSystemManager)
    kubectl = Mock(ProcessExecutor)
    config = new AppConfig(configLocation: '/app/config')
    executor = MoreExecutors.directExecutor()
    managedResourceCache = [:]
    subject = new ManagedResourcesService(fs, kubectl, config, executor, managedResourceCache)
  }

  def 'ManagedResourcesService requires valid params'() {
    when:
    new ManagedResourcesService(fsm, pe, conf, exec, cache)

    then:
    thrown RequireViolation

    where:
    fsm     | pe           | conf        | exec          | cache
    null    | null         | null        | null          | null
    this.fs | this.kubectl | null        | null          | null
    this.fs | this.kubectl | this.config | null          | null
    this.fs | this.kubectl | this.config | this.executor | null
  }

  def 'BootstrapManagedResources requires valid params'() {
    when:
    subject.bootstrapManagedResources(env, cluster)

    then:
    thrown RequireViolation

    where:
    env   | cluster
    null  | null
    ''    | null
    'env' | null
    'env' | ''
  }

  @PendingFeature
  def 'BootstrapManagedResources deploys clusterrolebinding, serviceaccount, generates customer kubeconfig and caches resources'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'

    // cluster exists with no managed resources
    subject.getManagedResources(environmentNm, clusterNm) >> []
    // kubeconfig file location
    String kubeFilePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"

    // mock for handlebars templateing calls to create namespace
    String cratekubeNamespacePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/resources/cratekube-namespace-namespace.yml"
    fs.resolveFile(cratekubeNamespacePath) >> Mock(FileObject) {
      getContent() >> Mock(FileContent) {
        getOutputStream() >> GroovyMock(OutputStream)
      }
    }

    // mock for handlebars templateing calls to create clusterrolebinding
    String clusterRoleBindingPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/resources/cluster-admin-clusterrolebinding.yml"
    fs.resolveFile(clusterRoleBindingPath) >> Mock(FileObject) {
      getContent() >> Mock(FileContent) {
        getOutputStream() >> GroovyMock(OutputStream)
      }
    }

    // mock for handlebars templateing calls to create serviceaccount
    String serviceaccountRoleBindingPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/resources/namespace-admin-serviceaccount.yml"
    fs.resolveFile(serviceaccountRoleBindingPath) >> Mock(FileObject) {
      getContent() >> Mock(FileContent) {
        getOutputStream() >> GroovyMock(OutputStream)
      }
    }

    // mock for handlebars templateing calls to create customer kubeconfig
    String customerKubeconfig = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/customer_kubeconfig.yml"
    fs.resolveFile(customerKubeconfig) >> Mock(FileObject) {
      getContent() >> Mock(FileContent) {
        getOutputStream() >> GroovyMock(OutputStream)
      }
    }

    when:
    subject.bootstrapManagedResources(environmentNm, clusterNm)

    then:
    1 * kubectl.exec("--kubeconfig ${kubeFilePath} apply -f ${cratekubeNamespacePath} -o json") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }

    1 * kubectl.exec("--kubeconfig ${kubeFilePath} apply -f ${clusterRoleBindingPath} -o json") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }

    1 * kubectl.exec("--kubeconfig ${kubeFilePath} apply -f ${serviceaccountRoleBindingPath} -o json") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }

    1 * kubectl.exec("--kubeconfig ${kubeFilePath} get serviceAccounts namespace-admin --namespace cratekube-namespace -o json") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }

    1 * kubectl.exec( allOf(startsWith("--kubeconfig ${kubeFilePath} get secrets"), endsWith('--namespace cratekube-namespace -o json'))) >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }

    1 * kubectl.exec("--kubeconfig ${kubeFilePath} config view --flatten --minify -o json") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }

    expect managedResourceCache["${environmentNm}/${clusterNm}".toString()], not(empty())
    expect managedResourceCache["${environmentNm}/${clusterNm}".toString()], hasSize(3)
    expect managedResourceCache["${environmentNm}/${clusterNm}".toString()], everyItem(hasProperty('status', equalTo(Status.COMPLETED)))
  }

  @PendingFeature
  def 'BootstrapManagedResources throws NotFoundException if cluster does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    // cluster does not exist
    subject.getManagedResources(environmentNm, clusterNm) >> {throw new NotFoundException()}

    when:
    subject.bootstrapManagedResources(environmentNm, clusterNm)

    then:
    thrown NotFoundException
  }

  @PendingFeature
  def 'BootstrapManagedResources throws AlreadyExistsException if any other managed resources exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    // a managed resources already exists
    subject.getManagedResources(environmentNm, clusterNm) >> [new ManagedResource()]

    when:
    subject.bootstrapManagedResources(environmentNm, clusterNm)

    then:
    thrown AlreadyExistsException
  }

  def 'DeployManagedResource requires valid params'() {
    when:
    subject.deployManagedResource(env, cluster, res)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | res
    null  | null      | null
    ''    | null      | null
    'env' | ''        | null
    'env' | null      | null
    'env' | 'cluster' | null
  }

  @PendingFeature
  def 'DeployManagedResource deploys a managed resource successfully'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'
    def managedRes = new ManagedResource(name: resourceNm, config: resourceCfg)
    //no resource exists matching resource-name
    subject.getManagedResource(environmentNm, clusterNm, resourceNm) >> null
    // kubeconfig file location
    String kubeFilePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    // config file location
    String managedResourcePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/resources/${resourceNm}.yml"

    when:
    subject.deployManagedResource(environmentNm, clusterNm, managedRes)

    then:
    1 * kubectl.exec("--kubeconfig ${kubeFilePath} apply -f ${managedResourcePath} -o json") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }
    1 * fs.resolveFile(managedResourcePath) >> Mock(FileObject) {
      1 * getContent() >> Mock(FileContent) {
        1 * getOutputStream() >> GroovyMock(OutputStream)
      }
    }
    expect managedResourceCache["${environmentNm}/${clusterNm}".toString()], not(empty())
    expect managedResourceCache["${environmentNm}/${clusterNm}".toString()], everyItem(hasProperty('status', equalTo(Status.COMPLETED)))
  }

  @PendingFeature
  def 'DeployManagedResource throws a NotFoundException if no cluster exists'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'
    def managedRes = new ManagedResource(name: resourceNm, config: resourceCfg)
    subject.getManagedResource(environmentNm, clusterNm, resourceNm) >> {throw new NotFoundException()}

    when:
    subject.deployManagedResource(environmentNm, clusterNm, managedRes)

    then:
    thrown NotFoundException
  }

  @PendingFeature
  def 'DeployManagedResource throws an AlreadyExistsException if the managed resource already exists'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'
    def managedRes = new ManagedResource(name: resourceNm, config: resourceCfg, status: Status.COMPLETED)
    subject.getManagedResource(environmentNm, clusterNm, resourceNm) >> managedRes

    when:
    subject.deployManagedResource(environmentNm, clusterNm, managedRes)

    then:
    thrown AlreadyExistsException
  }

  @PendingFeature
  def 'DeployManagedResource throws an InProgressException if the managed resource is being created or deleted'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'
    def managedRes = new ManagedResource(name: resourceNm, config: resourceCfg, status: Status.IN_PROGRESS)
    subject.getManagedResource(environmentNm, clusterNm, resourceNm) >> managedRes

    when:
    subject.deployManagedResource(environmentNm, clusterNm, managedRes)

    then:
    thrown InProgressException
  }

  def 'RemoveManagedResource requires valid params'() {
    when:
    subject.removeManagedResource(env, cluster, res)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | res
    null  | null      | null
    ''    | null      | null
    'env' | ''        | null
    'env' | null      | null
    'env' | 'cluster' | null
    'env' | 'cluster' | ''
  }

  @PendingFeature
  def 'RemoveManagedResource should successfully remove a managed resource that exists'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'
    def managedRes = new ManagedResource(name: resourceNm, config: resourceCfg, status: Status.COMPLETED)
    String kubeFilePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    String resourcePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/resources/${resourceNm}.yml"
    subject.getManagedResource(environmentNm, clusterNm, resourceNm) >> managedRes

    when:
    subject.removeManagedResource(environmentNm, clusterNm, resourceNm)

    then:
    1 * kubectl.exec("--kubeconfig ${kubeFilePath} delete -f ${resourcePath} -o json") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }
    1 * fs.resolveFile(resourcePath) >> Mock(FileObject) {
      1 * delete()
    }
    expect managedResourceCache["${environmentNm}/${clusterNm}".toString()], not(hasItem(hasProperty('name', equalTo(resourceNm))))
  }

  @PendingFeature
  def 'RemoveManagedResource should throw a NotFoundException if the cluster does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceNm = 'resource-name'
    subject.getManagedResource(environmentNm, clusterNm, resourceNm) >> {throw new NotFoundException()}

    when:
    subject.removeManagedResource(environmentNm, clusterNm, resourceNm)

    then:
    thrown NotFoundException
  }

  @PendingFeature
  def 'RemoveManagedResource should throw a NotFoundException if the resource does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceNm = 'resource-name'
    subject.getManagedResource(environmentNm, clusterNm, resourceNm) >> null

    when:
    subject.removeManagedResource(environmentNm, clusterNm, resourceNm)

    then:
    thrown InProgressException
  }

  @PendingFeature
  def 'RemoveManagedResource should  throw a InProgressException if the resource is processing'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'
    def managedRes = new ManagedResource(name: resourceNm, config: resourceCfg, status: Status.IN_PROGRESS)
    subject.getManagedResource(environmentNm, clusterNm, resourceNm) >> managedRes

    when:
    subject.removeManagedResource(environmentNm, clusterNm, resourceNm)

    then:
    thrown InProgressException
  }

  def 'GetManagedResource requires valid params'() {
    when:
    subject.getManagedResource(env, cluster, res)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | res
    null  | null      | null
    ''    | null      | null
    'env' | ''        | null
    'env' | null      | null
    'env' | 'cluster' | null
    'env' | 'cluster' | ''
  }

  @PendingFeature
  def 'GetManagedResource returns a managed resource when cached'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'
    def managedRes = new ManagedResource(name: resourceNm, config: resourceCfg, status: Status.COMPLETED)
    managedResourceCache["${environmentNm}/${clusterNm}".toString()] = [managedRes]

    when:
    def result = subject.getManagedResource(environmentNm, clusterNm, resourceNm)

    then:
    expect result, notNullValue()
    verifyAll(result) {
      expect name, equalTo(resourceNm)
      expect config, equalTo(resourceCfg)
      expect status, equalTo(Status.COMPLETED)
    }
  }

  @PendingFeature
  def 'GetManagedResource returns a managed resource when not cached and exists on filesystem'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'

    // mock kubeconfig exists on filesystem
    String kubeconfigPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    fs.resolveFile(kubeconfigPath) >> Mock(FileObject) {
      exists() >> true
    }

    String resourcePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/resources/${resourceNm}.yml"
    def inStream = GroovyMock(InputStream) {
      text >> resourceCfg
    }
    fs.resolveFile(resourcePath) >> Mock(FileObject) {
      exists() >> true
      getContent() >> Mock(FileContent) {
        getInputStream() >> inStream
      }
    }

    when:
    def result = subject.getManagedResource(environmentNm, clusterNm, resourceNm)

    then:
    expect result, notNullValue()
    verifyAll(result) {
      expect name, equalTo(resourceNm)
      expect config, equalTo(resourceCfg)
      expect status, equalTo(Status.COMPLETED)
    }
  }

  @PendingFeature
  def 'GetManagedResource throws a NotFoundException when the cluster does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceNm = 'resource-name'

    // mock kubeconfig does not exists on filesystem
    String kubeconfigPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    fs.resolveFile(kubeconfigPath) >> Mock(FileObject) {
      exists() >> false
    }

    when:
    subject.getManagedResource(environmentNm, clusterNm, resourceNm)

    then:
    thrown NotFoundException
  }

  def 'GetManagedResource returns null when the resource does not exist in cache or on filesystem'() {
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceNm = 'resource-name'
    // empty cache by default
    // mock kubeconfig exists on filesystem
    String kubeconfigPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    fs.resolveFile(kubeconfigPath) >> Mock(FileObject) {
      exists() >> true
    }

    String resourcePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/resources/${resourceNm}.yml"
    fs.resolveFile(resourcePath) >> Mock(FileObject) {
      exists() >> false
    }

    when:
    def result = subject.getManagedResource(environmentNm, clusterNm, resourceNm)

    then:
    expect result, nullValue()
  }

  def 'GetManagedResources requires valid params'() {
    when:
    subject.getManagedResources(env, cluster)

    then:
    thrown RequireViolation

    where:
    env   | cluster
    null  | null
    ''    | null
    'env' | null
    'env' | ''
  }

  @PendingFeature
  def 'GetManagedResources returns managed resource when cached'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'
    def managedRes = new ManagedResource(name: resourceNm, config: resourceCfg, status: Status.COMPLETED)
    managedResourceCache["${environmentNm}/${clusterNm}".toString()] = [managedRes]

    when:
    def result = subject.getManagedResources(environmentNm, clusterNm)

    then:
    expect result, notNullValue()
    expect result, hasItem(hasProperty('status', equalTo(Status.COMPLETED)))
    expect result, hasItem(hasProperty('config', equalTo(resourceCfg)))
    expect result, hasItem(hasProperty('name', equalTo(resourceNm)))
  }

  @PendingFeature
  def 'GetManagedResources returns managed resource when not cached but on filesystem'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceCfg = 'test resource config yaml'
    def resourceNm = 'resource-name'

    // mock kubeconfig exists on filesystem
    String kubeconfigPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    fs.resolveFile(kubeconfigPath) >> Mock(FileObject) {
      exists() >> true
    }

    String resourcePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/resources/${resourceNm}.yml"
    def inStream = GroovyMock(InputStream) {
      text >> resourceCfg
    }
    fs.resolveFile(resourcePath) >> Mock(FileObject) {
      exists() >> true
      getContent() >> Mock(FileContent) {
        getInputStream() >> inStream
      }
    }

    when:
    def result = subject.getManagedResources(environmentNm, clusterNm)

    then:
    expect result, notNullValue()
    expect result, hasItem(hasProperty('status', equalTo(Status.COMPLETED)))
    expect result, hasItem(hasProperty('config', equalTo(resourceCfg)))
    expect result, hasItem(hasProperty('name', equalTo(resourceNm)))
  }

  def 'GetManagedResources returns returns an empty list when not cached and not on filesystem'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resourceNm = 'resource-name'

    // mock kubeconfig exists on filesystem
    String kubeconfigPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    fs.resolveFile(kubeconfigPath) >> Mock(FileObject) {
      exists() >> true
    }

    String resourcePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/resources/${resourceNm}.yml"
    fs.resolveFile(resourcePath) >> Mock(FileObject) {
      exists() >> false
    }

    when:
    def result = subject.getManagedResources(environmentNm, clusterNm)

    then:
    expect result, notNullValue()
    expect result, is(empty())
  }

  @PendingFeature
  def 'GetManagedResources throws a NotFoundException when the cluster does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'

    // mock kubeconfig does not exists on filesystem
    String kubeconfigPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    fs.resolveFile(kubeconfigPath) >> Mock(FileObject) {
      exists() >> false
    }

    when:
    subject.getManagedResources(environmentNm, clusterNm)

    then:
    thrown NotFoundException
  }
}
