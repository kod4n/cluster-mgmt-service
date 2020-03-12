package io.cratekube.clustermgmt.resources

import io.cratekube.clustermgmt.BaseIntegrationSpec
import io.cratekube.clustermgmt.model.ManagedResource

import javax.ws.rs.core.GenericType

import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class ManagedResourcesResourceIntegrationTest extends BaseIntegrationSpec {
  String baseRequestPath = '/environment/test-env/cluster/test-cluster/resource'

  def 'should get list response when executing GET'() {
    when:
    def result = baseRequest().get(new GenericType<List<ManagedResource>>() {})

    then:
    expect result, notNullValue()
  }
}

