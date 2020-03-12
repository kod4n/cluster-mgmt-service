package io.cratekube.clustermgmt.resources

import io.cratekube.clustermgmt.BaseIntegrationSpec
import io.cratekube.clustermgmt.model.Cluster
import spock.lang.PendingFeature

import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class ClusterResourceIntegrationTest extends BaseIntegrationSpec {
  String baseRequestPath = '/environment/test-env/cluster/test-cluster'

  @PendingFeature
  def 'should get response when executing GET'() {
    when:
    def result = baseRequest().get(Cluster)

    then:
    expect result, notNullValue()
  }
}
