package io.cratekube.clustermgmt.resources

import io.cratekube.clustermgmt.BaseIntegrationSpec
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.model.Cluster
import io.cratekube.clustermgmt.model.ClusterNode
import io.cratekube.clustermgmt.model.Kubeconfig
import io.cratekube.clustermgmt.model.Status
import io.cratekube.clustermgmt.resources.request.BootstrapRequest

import static javax.ws.rs.client.Entity.json
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class ClusterResourceIntegrationSpec extends BaseIntegrationSpec {
  String environmentNm = 'test-env'
  String clusterNm = 'test-cluster'
  String baseRequestPath = "/environment/${environmentNm}/cluster"

  def 'Bootstrap request should return a 401 unauthorized with no admin auth token'() {
    given:
    def hostnms = ['test.io', 'test-2.io']
    def bootstrapRequest = new BootstrapRequest(clusterNm, hostnms)

    when:
    def response = baseRequest().post(json(bootstrapRequest))

    then:
    expect response, notNullValue()
    expect response.status, equalTo(401)
  }

  def 'Bootstrap request should return a 201 created response with location header set'() {
    given:
    def hostnms = ['test.io', 'test-2.io']
    def bootstrapRequest = new BootstrapRequest(clusterNm, hostnms)

    when:
    def response = requestWithAdminToken().post(json(bootstrapRequest))

    then:
    expect response, notNullValue()
    verifyAll(response) {
      expect status, equalTo(201)
      expect location, notNullValue()
      expect location.path, containsString("/environment/${environmentNm}/cluster/${clusterNm}")
    }
  }

  def 'Bootstrap request should return a 409 conflict response if a bootstrap is in progress for a cluster'() {
    given:
    def hostnms = ['test.io', 'test-2.io']
    def bootstrapRequest = new BootstrapRequest(clusterNm, hostnms)
    clusters.bootstrapCluster(environmentNm, clusterNm, hostnms) >> {throw new InProgressException()}

    when:
    def response = requestWithAdminToken().post(json(bootstrapRequest))

    then:
    expect response, notNullValue()
    expect response.status, equalTo(409)
  }

  def 'Bootstrap request should return a 409 conflict response if cluster already exists'() {
    given:
    def hostnms = ['test.io', 'test-2.io']
    def bootstrapRequest = new BootstrapRequest(clusterNm, hostnms)
    clusters.bootstrapCluster(environmentNm, clusterNm, hostnms) >> {throw new AlreadyExistsException()}

    when:
    def response = requestWithAdminToken().post(json(bootstrapRequest))

    then:
    expect response, notNullValue()
    expect response.status, equalTo(409)
  }

  def 'Delete request should return a 401 unauthorized with no admin auth token'() {
    when:
    def response = baseRequest("/${clusterNm}").delete()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(401)
  }

  def 'Delete request should return a 202 accepted response with location header set'() {
    when:
    def response = requestWithAdminToken("/${clusterNm}").delete()

    then:
    expect response, notNullValue()
    verifyAll(response) {
      expect status, equalTo(202)
      expect location, notNullValue()
      expect location.path, containsString("/environment/${environmentNm}/cluster/${clusterNm}")
    }
  }

  def 'Delete request should return a 404 not found response if cluster does not exist'() {
    given:
    clusters.destroyCluster(environmentNm, clusterNm) >> {throw new NotFoundException()}

    when:
    def response = requestWithAdminToken("/${clusterNm}").delete()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(404)
  }

  def 'Delete request should return a 409 conflict response if cluster bootstrap is in progress'() {
    given:
    clusters.destroyCluster(environmentNm, clusterNm) >> {throw new InProgressException()}

    when:
    def response = requestWithAdminToken("/${clusterNm}").delete()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(409)
  }

  def 'Get cluster request should return a 200 response and a cluster'() {
    given:
    def clusterConfig = 'cluster config yaml'
    def clusterNodes = [new ClusterNode(hostname: 'test.io', status: Status.IN_PROGRESS)]
    def cluster = new Cluster(
      envName: environmentNm,
      name: clusterNm,
      config: clusterConfig,
      nodes: clusterNodes
    )
    clusters.getCluster(environmentNm, clusterNm) >> cluster
    when:
    def response = baseRequest("/${clusterNm}").get()
    def clusterResponse = response.readEntity(Cluster)

    then:
    expect response, notNullValue()
    expect response.status, equalTo(200)
    verifyAll(clusterResponse) {
      expect envName, equalTo(environmentNm)
      expect name, equalTo(clusterNm)
      expect config, equalTo(clusterConfig)
      expect clusterNodes, everyItem(hasProperty('status', equalTo(Status.IN_PROGRESS)))
    }
  }

  def 'Get cluster request should return a 404 not found if a cluster does not exist'() {
    given:
    clusters.getCluster(environmentNm, clusterNm) >> {throw new NotFoundException()}

    when:
    def response = baseRequest("/${clusterNm}").get()

    then:
    expect response, notNullValue()
    expect response.status, equalTo(404)
  }

  def 'Get customer kubeconfig request should return a 200 response and kubeconfig'() {
    given:
    def customerKubeconfig = new Kubeconfig('kubeconfig String')
    clusters.getCustomerKubeconfig(environmentNm, clusterNm) >> customerKubeconfig

    when:
    def response = baseRequest("/${clusterNm}/kubeconfig/customer").get()
    def customerKubeconfigResponse = response.readEntity(Kubeconfig)
    then:
    expect response, notNullValue()
    expect response.status, equalTo(200)
    expect customerKubeconfigResponse, equalTo(customerKubeconfig)
  }

  def 'Get customer kubeconfig request should return a 404 not found response if no cluster exists'() {
    given:
    clusters.getCustomerKubeconfig(environmentNm, clusterNm) >> {throw new NotFoundException()}

    when:
    def response = baseRequest("/${clusterNm}/kubeconfig/customer").get()
    then:
    expect response, notNullValue()
    expect response.status, equalTo(404)
  }

  def 'Get customer kubeconfig request should return a 409 conflict response if the cluster bootstrap is still processing'() {
    given:
    clusters.getCustomerKubeconfig(environmentNm, clusterNm) >> {throw new InProgressException()}

    when:
    def response = baseRequest("/${clusterNm}/kubeconfig/customer").get()
    then:
    expect response, notNullValue()
    expect response.status, equalTo(409)
  }
}
