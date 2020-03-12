package io.cratekube.clustermgmt.resources

import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.model.ManagedResource
import org.valid4j.errors.RequireViolation
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class ManagedResourcesResourceTest extends Specification {
  @Subject ManagedResourcesResource subject
  ManagedResourcesApi resourcesApi

  def setup() {
    resourcesApi = Mock(ManagedResourcesApi)
    subject = new ManagedResourcesResource(resourcesApi)
  }

  def 'ManagedResourcesResource requires valid params'() {
    when:
    new ManagedResourcesResource(null)

    then:
    thrown RequireViolation
  }

  def 'CreateManagedResource requires valid params'() {
    when:
    subject.createManagedResource(env, cluster, req)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | req
    null  | null      | null
    ''    | null      | null
    'env' | null      | null
    'env' | ''        | null
    'env' | 'cluster' | null
  }

  @PendingFeature
  def 'CreateManagedResource returns a valid result'() {
    when:
    def result = subject.createManagedResource('test-env', 'test-cluster', new ManagedResource(name:'test-resource', config: 'test-config'))

    then:
    expect result, notNullValue()
    expect result.status, equalTo(201)
  }

  def 'DeleteManagedResource requires valid params'() {
    when:
    subject.deleteManagedResource(env, cluster, resource)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | resource
    null  | null      | null
    ''    | null      | null
    'env' | null      | null
    'env' | ''        | null
    'env' | 'cluster' | null
    'env' | 'cluster' | ''
  }

  @PendingFeature
  def 'DeleteManagedResource returns a valid result'() {
    when:
    def result = subject.deleteManagedResource('test-env', 'test-cluster', 'test-resource')

    then:
    expect result, notNullValue()
    expect result.status, equalTo(202)
  }

  def 'GetManagedResource requires valid params'() {
    when:
    subject.getManagedResource(env, cluster, resource)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | resource
    null  | null      | null
    ''    | null      | null
    'env' | null      | null
    'env' | ''        | null
    'env' | 'cluster' | null
    'env' | 'cluster' | ''
  }

  @PendingFeature
  def 'GetManagedResource returns a valid result'() {
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
}
