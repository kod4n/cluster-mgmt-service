package io.cratekube.clustermgmt.service

import com.google.common.util.concurrent.MoreExecutors
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.model.ManagedResource
import org.apache.commons.vfs2.FileSystemManager
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.Executor

import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.not
import static spock.util.matcher.HamcrestSupport.expect
import static org.hamcrest.Matchers.notNullValue

class ManagedResourcesServiceTest extends Specification {
  @Subject ManagedResourcesService subject
  FileSystemManager fs
  ProcessExecutor kubectl
  AppConfig config
  Executor executor
  Map<String, List<ManagedResource>> managedResourceCache

  def setup() {
    fs = Mock(FileSystemManager)
    kubectl = Mock(ProcessExecutor)
    config = new AppConfig()
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
  def 'GetManagedResource returns a managed resource'() {
    when:
    def result = subject.getManagedResource('test-env', 'test-cluster', 'test-resource')

    then:
    expect result, notNullValue()
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
  def 'GetManagedResources returns a clusters managed resources'() {
    when:
    def result = subject.getManagedResources('test-env', 'test-cluster')

    then:
    expect result, not(empty())
  }
}
