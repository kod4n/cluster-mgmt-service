package io.cratekube.clustermgmt.service

import com.google.common.util.concurrent.MoreExecutors
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.config.RkeConfig
import io.cratekube.clustermgmt.model.Cluster
import io.cratekube.clustermgmt.model.ClusterNode
import io.cratekube.clustermgmt.model.Status
import org.apache.commons.vfs2.FileContent
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.Executor

import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasKey
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.nullValue
import static spock.util.matcher.HamcrestSupport.expect
import static org.hamcrest.Matchers.notNullValue

class ClusterServiceSpec extends Specification {
  @Subject ClusterService subject
  FileSystemManager fs
  ProcessExecutor rke
  ManagedResourcesApi managedResourcesApi
  AppConfig config
  Executor executor
  Map<String, Cluster> clusterCache

  def setup() {
    fs = Mock(FileSystemManager)
    rke = Mock(ProcessExecutor)
    managedResourcesApi = Mock(ManagedResourcesApi)

    config = new AppConfig(configLocation: '/app/config', rke: new RkeConfig(nodeUser: 'ssh-user', sshKeyPath: '~/.ssh_id_rsa'))
    executor = MoreExecutors.directExecutor()
    clusterCache = [:]
    subject = new ClusterService(fs, rke, managedResourcesApi, config, executor, clusterCache)
  }

  def 'ClusterService requires valid params'() {
    when:
    new ClusterService(fsm, pe, mra, conf, exec, cache)

    then:
    thrown RequireViolation

    where:
    fsm     | pe       | mra                      | conf        | exec          | cache
    null    | null     | null                     | null        | null          | null
    this.fs | null     | null                     | null        | null          | null
    this.fs | this.rke | null                     | null        | null          | null
    this.fs | this.rke | this.managedResourcesApi | null        | null          | null
    this.fs | this.rke | this.managedResourcesApi | this.config | null          | null
    this.fs | this.rke | this.managedResourcesApi | this.config | this.executor | null
  }

  def 'BootstrapCluster requires valid params'() {
    when:
    subject.bootstrapCluster(env, cluster, hosts)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | hosts
    null  | null      | null
    ''    | null      | null
    'env' | null      | null
    'env' | ''        | null
    'env' | 'cluster' | null
    'env' | 'cluster' | []
    'env' | 'cluster' | ['', ' ']
  }

  @PendingFeature
  def 'BootstrapCluster should bootstrap a new cluster and place it in cache when one does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def hostnms = ['test.io', 'test-2.io']
    // cluster does not exist
    subject.getCluster(environmentNm, clusterNm) >> {throw new NotFoundException()}

    // mock for handlebars templateing calls to create cluster config
    String clusterFilePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/cluster.yml"
    fs.resolveFile(clusterFilePath) >> Mock(FileObject) {
      getContent() >> Mock(FileContent) {
        getOutputStream() >> GroovyMock(OutputStream)
      }
    }

    when:
    subject.bootstrapCluster(environmentNm, clusterNm, hostnms)

    then:
    1 * rke.exec('up', '--config', "${clusterFilePath}") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }
    1 * managedResourcesApi.bootstrapManagedResources(environmentNm, clusterNm)
    expect clusterCache, hasKey("${environmentNm}/${clusterNm}".toString())
    expect clusterCache["${environmentNm}/${clusterNm}".toString()].nodes, everyItem(hasProperty('status', equalTo(Status.COMPLETED)))
  }

  @PendingFeature
  def 'BootstrapCluster should throw InProgressException if cluster operation is in progress'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def hostnms = ['test.io', 'test-2.io']
    // cluster bootstrap is in progress
    subject.getCluster(environmentNm, clusterNm) >> new Cluster(nodes: hostnms.collect {new ClusterNode(it, Status.IN_PROGRESS)})

    when:
    subject.bootstrapCluster(environmentNm, clusterNm, hostnms)

    then:
    thrown InProgressException
  }

  @PendingFeature
  def 'BootstrapCluster should throw AlreadyExistsException if cluster already exists in cache as completed'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def hostnms = ['test.io', 'test-2.io']
    subject.getCluster(environmentNm, clusterNm) >> new Cluster(nodes: hostnms.collect {new ClusterNode(it, Status.COMPLETED)})

    when:
    subject.bootstrapCluster(environmentNm, clusterNm, hostnms)

    then:
    thrown AlreadyExistsException
  }

  def 'DestroyCluster requires valid params'() {
    when:
    subject.destroyCluster(env, cluster)

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
  def 'DestroyCluster should destroy a cluster, remove file and null in cache'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def hostnms = ['test.io', 'test-2.io']
    String clusterPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}"
    String clusterFilePath = "${clusterPath}/cluster.yml"
    subject.getCluster(environmentNm, clusterNm) >> new Cluster(nodes: hostnms.collect {new ClusterNode(it, Status.COMPLETED)})

    when:
    subject.destroyCluster(environmentNm, clusterNm)

    then:
    1 * rke.exec('remove', '--config', "${clusterFilePath}") >> GroovyMock(Process) {
      1 * waitForProcessOutput(_ as OutputStream, _ as OutputStream)
      exitValue() >> 0
    }
    1 * fs.resolveFile(clusterPath) >> Mock(FileObject) {
      1 * delete()
    }
    expect clusterCache["${environmentNm}/${clusterNm}".toString()], nullValue()
  }

  @PendingFeature
  def 'DestroyCluster should throw InProgressException if cluster operation is in progress'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def hostnms = ['test.io', 'test-2.io']
    // cluster bootstrap is in progress
    subject.getCluster(environmentNm, clusterNm) >> new Cluster(nodes: hostnms.collect {new ClusterNode(it, Status.IN_PROGRESS)})

    when:
    subject.destroyCluster(environmentNm, clusterNm)

    then:
    thrown InProgressException
  }

  @PendingFeature
  def 'DestroyCluster should throw NotFoundException if cluster is not found'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    // cluster bootstrap is in progress
    subject.getCluster(environmentNm, clusterNm) >> {throw new NotFoundException()}

    when:
    subject.destroyCluster(environmentNm, clusterNm)

    then:
    thrown NotFoundException
  }

  def 'GetCluster requires valid params'() {
    when:
    subject.getCluster(env, cluster)

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
  def 'GetCluster returns a cluster when one exist on filesystem and not in cache'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def clusterCfg = /nodes:
                      |  - address: test.io
                      |    user: ssh-user
                      |    roles: [controlplane, etcd]
                      |  - address: test-2.io
                      |    user: ssh-user
                      |    roles: [worker]
                      |ssh_key_path: ~\/.ssh_id_rsa
                      |services:
                      |  etcd:
                      |    snapshot: true
                      |    creation: 6h
                      |    retention: 24h
                      |  kube-api:
                      |    pod_security_policy: true
                      |    always_pull_images: true/.stripMargin()
    // cache defaults to empty
    // mock kubeconfig exists on filesystem
    String kubeconfigPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    fs.resolveFile(kubeconfigPath) >> Mock(FileObject) {
      exists() >> true
    }
    // mock cluster config from filesystem
    String clusterFilePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/cluster.yml"
    def inStream = GroovyMock(InputStream) {
      text >> clusterCfg
    }
    fs.resolveFile(clusterFilePath) >> Mock(FileObject) {
      getContent() >> Mock(FileContent) {
        getInputStream() >> inStream
      }
    }

    when:
    def cluster = subject.getCluster('test-env', 'test-cluster')

    then:
    expect cluster, notNullValue()
    verifyAll(cluster) {
      expect cluster.name, equalTo(clusterNm)
      expect cluster.envName, equalTo(environmentNm)
      expect cluster.config, equalTo(clusterCfg)
      expect cluster.nodes, containsInAnyOrder(['test.io', 'test-2.io'].collect {new ClusterNode(it, Status.COMPLETED)}.toArray())
    }
  }

  @PendingFeature
  def 'GetCluster returns a cluster when one exist in cache'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def clusterCfg = 'test cluster config yaml'
    def hostnms = ['test.io', 'test-2.io']
    clusterCache["${environmentNm}/${clusterNm}".toString()] = new Cluster(environmentNm, clusterNm, clusterCfg, hostnms.collect {new ClusterNode(it, Status.COMPLETED)})

    when:
    def cluster = subject.getCluster(environmentNm, clusterNm)

    then:
    expect cluster, notNullValue()
    verifyAll(cluster) {
      expect cluster.name, equalTo(clusterNm)
      expect cluster.envName, equalTo(environmentNm)
      expect cluster.config, equalTo(clusterCfg)
      expect cluster.nodes, containsInAnyOrder(['test.io', 'test-2.io'].collect {new ClusterNode(it, Status.COMPLETED)}.toArray())
    }
  }

  @PendingFeature
  def 'GetCluster throws NotFoundException when no cluster exists in cache or on filesystem'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    // cache defaults to empty
    // mock kubeconfig does not exists on filesystem
    String kubeFilePath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/kube_config_cluster.yml"
    fs.resolveFile(kubeFilePath) >> Mock(FileObject) {
      exists() >> false
    }

    when:
    subject.getCluster(environmentNm, clusterNm)

    then:
    thrown NotFoundException
  }

  def 'GetCustomerKubeconfig requires valid params'() {
    when:
    subject.getCustomerKubeconfig(env, cluster)

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
  def 'GetCustomerKubeconfig returns a customer kubeconfig'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def clusterCfg = 'test cluster config yaml'
    def hostnms = ['test.io', 'test-2.io']
    subject.getCluster(environmentNm, clusterNm) >> new Cluster(environmentNm, clusterNm, clusterCfg, hostnms.collect {new ClusterNode(it, Status.COMPLETED)})

    String customerKubeconfigPath = "${config.configLocation}/environment/${environmentNm}/cluster/${clusterNm}/customer-kubeconfig.yml"
    def inStream = GroovyMock(InputStream) {
      text >> clusterCfg
    }
    fs.resolveFile(customerKubeconfigPath) >> Mock(FileObject) {
      getContent() >> Mock(FileContent) {
        getInputStream() >> inStream
      }
    }

    when:
    def result = subject.getCustomerKubeconfig(environmentNm, clusterNm)

    then:
    expect result, notNullValue()
    expect result.kubeconfig, equalTo(clusterCfg)
  }

  @PendingFeature
  def 'GetCustomerKubeconfig throws a NotFoundException if a cluster does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    subject.getCluster(environmentNm, clusterNm) >> {throw new NotFoundException()}

    when:
    subject.getCustomerKubeconfig(environmentNm, clusterNm)

    then:
    thrown NotFoundException
  }

  @PendingFeature
  def 'GetCustomerKubeconfig throws a InProgressException if a cluster operation is in progress'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def clusterCfg = 'test cluster config yaml'
    def hostnms = ['test.io', 'test-2.io']
    subject.getCluster(environmentNm, clusterNm) >> new Cluster(environmentNm, clusterNm, clusterCfg, hostnms.collect {new ClusterNode(it, Status.IN_PROGRESS)})

    when:
    subject.getCustomerKubeconfig(environmentNm, clusterNm)

    then:
    thrown InProgressException
  }
}
