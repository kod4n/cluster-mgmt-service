package io.cratekube.clustermgmt.resources

import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.dropwizard.auth.User
import io.cratekube.clustermgmt.model.ManagedResource
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import static io.cratekube.clustermgmt.model.Status.COMPLETED
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class ManagedResourcesResourceSpec extends Specification {
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
    subject.createManagedResource(env, cluster, req, user)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | req                   | user
    null  | null      | null                  | null
    ''    | null      | null                  | null
    'env' | null      | null                  | null
    'env' | ''        | null                  | null
    'env' | 'cluster' | null                  | null
    'env' | 'cluster' | new ManagedResource() | null
  }

  def 'CreateManagedResource returns a valid result'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resName = 'test-resource'
    def mngdRes = new ManagedResource(name: resName, config: 'test-config')

    when:
    def result = subject.createManagedResource(environmentNm, clusterNm, mngdRes, new User())

    then:
    1 * resourcesApi.deployManagedResource(environmentNm, clusterNm, mngdRes)
    expect result, notNullValue()
    verifyAll(result) {
      expect status, equalTo(201)
      expect location, notNullValue()
      expect location.path, containsString("/environment/${environmentNm}/cluster/${clusterNm}/resource/${resName}")
    }
  }

  def 'CreateManagedResource throws a InProgressException if the managed resource creation is in progress'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def mngdRes = new ManagedResource(name: 'test-resource', config: 'test-config')
    resourcesApi.deployManagedResource(environmentNm, clusterNm, mngdRes)  >> {throw new InProgressException()}

    when:
    subject.createManagedResource(environmentNm, clusterNm, mngdRes, new User())

    then:
    thrown InProgressException
  }

  def 'CreateManagedResource throws a AlreadyExistsException if the managed resource already exists'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def mngdRes = new ManagedResource(name: 'test-resource', config: 'test-config')
    resourcesApi.deployManagedResource(environmentNm, clusterNm, mngdRes)  >> {throw new AlreadyExistsException()}

    when:
    subject.createManagedResource(environmentNm, clusterNm, mngdRes, new User())

    then:
    thrown AlreadyExistsException
  }

  def 'CreateManagedResource throws a NotFoundException if the cluster does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def mngdRes = new ManagedResource(name: 'test-resource', config: 'test-config')
    resourcesApi.deployManagedResource(environmentNm, clusterNm, mngdRes)  >> {throw new NotFoundException()}

    when:
    subject.createManagedResource(environmentNm, clusterNm, mngdRes, new User())

    then:
    thrown NotFoundException
  }

  def 'DeleteManagedResource requires valid params'() {
    when:
    subject.deleteManagedResource(env, cluster, req, user)

    then:
    thrown RequireViolation

    where:
    env   | cluster   | req        | user
    null  | null      | null       | null
    ''    | null      | null       | null
    'env' | null      | null       | null
    'env' | ''        | null       | null
    'env' | 'cluster' | null       | null
    'env' | 'cluster' | ''         | null
    'env' | 'cluster' | 'resource' | null
  }

  def 'DeleteManagedResource returns a valid result'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resName = 'test-resource'

    when:
    def result = subject.deleteManagedResource(environmentNm, clusterNm, resName, new User())

    then:
    1 * resourcesApi.removeManagedResource(environmentNm, clusterNm, resName)
    expect result, notNullValue()
    verifyAll(result) {
      expect status, equalTo(202)
      expect location, notNullValue()
      expect location.path, containsString("/environment/${environmentNm}/cluster/${clusterNm}/resource/${resName}")
    }
  }

  def 'DeleteManagedResource throws a InProgressException if the managed resource creation is in progress'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resName = 'test-resource'
    resourcesApi.removeManagedResource(environmentNm, clusterNm, resName)  >> {throw new InProgressException()}

    when:
    subject.deleteManagedResource(environmentNm, clusterNm, resName, new User())

    then:
    thrown InProgressException
  }

  def 'DeleteManagedResource throws a NotFoundException if the cluster or managed resource does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resName = 'test-resource'
    resourcesApi.removeManagedResource(environmentNm, clusterNm, resName)  >> {throw new NotFoundException()}

    when:
    subject.deleteManagedResource(environmentNm, clusterNm, resName, new User())

    then:
    thrown NotFoundException
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

  def 'GetManagedResource returns a valid result'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resName = 'test-resource'
    def mngdRes = new ManagedResource(name: resName, config: 'test-config', status: COMPLETED)

    when:
    def result = subject.getManagedResource(environmentNm, clusterNm, resName)

    then:
    1 * resourcesApi.getManagedResource(environmentNm, clusterNm, resName) >> mngdRes
    expect result, notNullValue()
    expect result, equalTo(mngdRes)
  }

  def 'GetManagedResource throws a NotFoundException if the cluster does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resName = 'test-resource'
    resourcesApi.getManagedResource(environmentNm, clusterNm, resName)  >> {throw new NotFoundException()}

    when:
    subject.getManagedResource(environmentNm, clusterNm, resName)

    then:
    thrown NotFoundException
  }

  def 'GetManagedResource throws a NotFoundException if the managed resource does not exist'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def resName = 'test-resource'
    resourcesApi.getManagedResource(environmentNm, clusterNm, resName) >> null

    when:
    subject.getManagedResource(environmentNm, clusterNm, resName)

    then:
    thrown NotFoundException
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

  def "GetManagedResources returns valid results"() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def mngdRes = [
      new ManagedResource(name: 'test-resource', config: 'test-config', status: COMPLETED),
      new ManagedResource(name: 'test-resource-2', config: 'test-config-2', status: COMPLETED)
    ]

    when:
    def result = subject.getManagedResources(environmentNm, clusterNm)

    then:
    1 * resourcesApi.getManagedResources(environmentNm, clusterNm) >> mngdRes
    expect result, notNullValue()
    expect result, equalTo(mngdRes)
  }

  def 'GetManagedResources throws a NotFoundException if no cluster exists'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    resourcesApi.getManagedResources(environmentNm, clusterNm)  >> {throw new NotFoundException()}

    when:
    subject.getManagedResources(environmentNm, clusterNm)

    then:
    thrown NotFoundException
  }
}
