package io.cratekube.clustermgmt.service

import com.google.common.util.concurrent.MoreExecutors
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.model.Cluster
import org.apache.commons.vfs2.FileSystemManager
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.Executor

import static spock.util.matcher.HamcrestSupport.expect
import static org.hamcrest.Matchers.notNullValue

class ClusterServiceTest extends Specification {
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
    config = new AppConfig()
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
  def 'GetCluster returns a cluster'() {
    when:
    def result = subject.getCluster('test-env', 'test-cluster')

    then:
    expect result, notNullValue()
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
    when:
    def result = subject.getCustomerKubeconfig('test-env', 'test-cluster')

    then:
    expect result, notNullValue()
  }
}
