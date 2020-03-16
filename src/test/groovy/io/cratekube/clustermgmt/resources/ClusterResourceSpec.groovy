package io.cratekube.clustermgmt.resources

import io.cratekube.clustermgmt.api.ClusterApi
import io.cratekube.clustermgmt.dropwizard.auth.User
import io.cratekube.clustermgmt.resources.request.BootstrapRequest
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.equalTo
import static spock.util.matcher.HamcrestSupport.expect
import static org.hamcrest.Matchers.notNullValue

class ClusterResourceSpec extends Specification {
  @Subject ClusterResource subject
  ClusterApi clusters

  def setup() {
    clusters = Mock(ClusterApi)
    subject = new ClusterResource(clusters)
  }

  def 'ClusterResource requires valid parameters'() {
    when:
    new ClusterResource(null)

    then:
    thrown RequireViolation
  }

  def 'BootstrapCluster requires valid parameters'() {
    when:
    subject.bootstrapCluster(env, req, new User())

    then:
    thrown RequireViolation

    where:
    env   | req
    null  | null
    ''    | null
    'env' | null
  }

  @PendingFeature
  def 'BootstrapCluster returns a valid result'() {
    when:
    def result = subject.bootstrapCluster('test-env', new BootstrapRequest(clusterName: 'test-cluster', hostnames: ['test.io', 'test-2.io']), new User())

    then:
    expect result, notNullValue()
    expect result.status, equalTo(201)
  }

  def 'DeleteCluster requires valid parameters'() {
    when:
    subject.deleteCluster(env, cluster, user)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | user
    null  | null      | null
    ''    | null      | null
    'env' | null      | null
    'env' | ''        | null
    'env' | 'cluster' | null
  }

  @PendingFeature
  def 'DeleteCluster returns a valid result'() {
    when:
    def result = subject.deleteCluster('test-env', 'test-cluster', new User())

    then:
    expect result, notNullValue()
    expect result.status, equalTo(202)
  }

  def 'GetCluster requires valid parameters'() {
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

  def 'GetCustomerKubeconfig requires valid parameters'() {
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
  def 'GetCustomerKubeconfig returns a valid result'() {
    when:
    def result = subject.getCustomerKubeconfig('test-env', 'test-cluster')

    then:
    expect result, notNullValue()
  }
}
