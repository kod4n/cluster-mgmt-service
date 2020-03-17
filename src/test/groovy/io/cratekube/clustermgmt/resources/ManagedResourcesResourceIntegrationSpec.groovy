package io.cratekube.clustermgmt.resources

import io.cratekube.clustermgmt.BaseIntegrationSpec
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.model.ManagedResource
import io.cratekube.clustermgmt.model.Status
import spock.lang.PendingFeature

import javax.ws.rs.core.GenericType

import static javax.ws.rs.client.Entity.json
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class ManagedResourcesResourceIntegrationSpec extends BaseIntegrationSpec {
  String environmentNm = 'test-env'
  String clusterNm = 'test-cluster'
  String resourceNm = 'test-resource'
  String resourceCfg = 'test resource config'
  String baseRequestPath = "/environment/${environmentNm}/cluster/${clusterNm}/resource"

  def 'Managed resource create request should return a 401 unauthorized with no admin auth token'() {
    given:
    def mngdRes = new ManagedResource(name: resourceNm, config: resourceCfg)

    when:
    def response = baseRequest().post(json(mngdRes))

    then:
    expect response, notNullValue()
    expect response.status, equalTo(401)
  }

  @PendingFeature
  def 'Managed resource create request should return a 201 created response with location header set'() {
    given:
    def mngdRes = new ManagedResource(name: resourceNm, config: resourceCfg)

    when:
    def response = requestWithAdminToken().post(json(mngdRes))

    then:
    expect response, notNullValue()
    verifyAll(response) {
      expect status, equalTo(201)
      expect location, notNullValue()
      expect location.path, containsString("/environment/${environmentNm}/cluster/${clusterNm}/resource/${resourceNm}")
    }
  }

  @PendingFeature
  def 'Managed resource create request should return a 404 not found response if no cluster exists'() {
    given:
    def mngdRes = new ManagedResource(name: resourceNm, config: resourceCfg)
    resources.deployManagedResource(environmentNm, clusterNm, mngdRes) >> {throw new NotFoundException()}

    when:
    def response = requestWithAdminToken().post(json(mngdRes))

    then:
    expect response, notNullValue()
    expect response.status, equalTo(404)
  }

  @PendingFeature
  def 'Managed resource create request should return a 409 conflict response if the managed resource creation is in progress'() {
    given:
    def mngdRes = new ManagedResource(name: resourceNm, config: resourceCfg)
    resources.deployManagedResource(environmentNm, clusterNm, mngdRes) >> {throw new InProgressException()}

    when:
    def response = requestWithAdminToken().post(json(mngdRes))

    then:
    expect response, notNullValue()
    expect response.status, equalTo(409)
  }

  @PendingFeature
  def 'Managed resource create request should return a 409 conflict response if the managed resource already exists'() {
    given:
    def mngdRes = new ManagedResource(name: resourceNm, config: resourceCfg)
    resources.deployManagedResource(environmentNm, clusterNm, mngdRes) >> {throw new AlreadyExistsException()}

    when:
    def response = requestWithAdminToken().post(json(mngdRes))

    then:
    expect response, notNullValue()
    expect response.status, equalTo(409)
  }

  def 'Managed resource delete request should return a 401 unauthorized with no admin auth token'() {
    when:
    def response = baseRequest("/${resourceNm}").delete()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(401)
  }

  @PendingFeature
  def 'Managed resource delete request should return a 202 accepted response with location header set'() {
    when:
    def response = requestWithAdminToken("/${resourceNm}").delete()

    then:
    expect response, notNullValue()
    verifyAll(response) {
      expect status, equalTo(202)
      expect location, notNullValue()
      expect location.path, containsString("/environment/${environmentNm}/cluster/${clusterNm}/resource/${resourceNm}")
    }
  }

  @PendingFeature
  def 'Managed resource delete request should return a 404 not found response if no cluster exists'() {
    given:
    resources.removeManagedResource(environmentNm, clusterNm, resourceNm) >> {throw new NotFoundException()}

    when:
    def response = requestWithAdminToken("/${resourceNm}").delete()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(404)
  }

  @PendingFeature
  def 'Managed resource delete request should return a 409 conflict response if the managed resource creation is in progress'() {
    given:
    resources.deployManagedResource(environmentNm, clusterNm, resourceNm) >> {throw new InProgressException()}

    when:
    def response = requestWithAdminToken("/${resourceNm}").delete()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(409)
  }

  @PendingFeature
  def 'Managed resource get request should return a 200 response and the managed resource'() {
    given:
    def mngdRes = new ManagedResource(name: resourceNm, config: resourceCfg, status: Status.COMPLETED)
    resources.getManagedResource(environmentNm, clusterNm, resourceNm) >> mngdRes

    when:
    def response = baseRequest("/${resourceNm}").delete()
    def mngdResResponse = response.readEntity(ManagedResource)
    then:
    expect response, notNullValue()
    expect response.status, equalTo(200)
    expect mngdResResponse, equalTo(mngdRes)
  }

  @PendingFeature
  def 'Managed resource get request should return a 404 not found response if no cluster exists'() {
    given:
    resources.getManagedResource(environmentNm, clusterNm, resourceNm) >> {throw new NotFoundException()}

    when:
    def response = baseRequest("/${resourceNm}").get()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(404)
  }

  @PendingFeature
  def 'Managed resource get request should return a 404 not found response if managed resource does not exist'() {
    given:
    resources.getManagedResource(environmentNm, clusterNm, resourceNm) >> null

    when:
    def response = baseRequest("/${resourceNm}").get()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(404)
  }

  @PendingFeature
  def 'Managed resources get request should return a 200 response and the managed resource'() {
    given:
    def mngdRes = [
      new ManagedResource(name: resourceNm, config: resourceCfg, status: Status.COMPLETED),
      new ManagedResource(name: 'test-resource-2', config: 'test resource config 2', status: Status.COMPLETED)
    ]
    resources.getManagedResource(environmentNm, clusterNm, resourceNm) >> mngdRes

    when:
    def response = baseRequest().get()
    def mngdResResponse = response.readEntity(new GenericType<List<ManagedResource>>() {})

    then:
    expect response, notNullValue()
    expect response.status, equalTo(200)
    expect mngdResResponse, equalTo(mngdRes)
  }

  @PendingFeature
  def 'Managed resources get request should return a 404 not found response if no cluster exists'() {
    given:
    resources.getManagedResource(environmentNm, clusterNm, resourceNm) >> {throw new NotFoundException()}

    when:
    def response = baseRequest().get()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(404)
  }
}

