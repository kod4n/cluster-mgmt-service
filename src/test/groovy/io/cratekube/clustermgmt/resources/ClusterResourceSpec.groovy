package io.cratekube.clustermgmt.resources

import io.cratekube.clustermgmt.api.ClusterApi
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.dropwizard.auth.User
import io.cratekube.clustermgmt.model.Cluster
import io.cratekube.clustermgmt.model.ClusterNode
import io.cratekube.clustermgmt.model.Kubeconfig
import io.cratekube.clustermgmt.model.Status
import io.cratekube.clustermgmt.resources.request.BootstrapRequest
import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.containsString
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
    subject.bootstrapCluster(env, req, user)

    then:
    thrown RequireViolation

    where:
    env   | req                    | user
    null  | null                   | null
    ''    | null                   | null
    'env' | null                   | null
    'env' | new BootstrapRequest() | null
  }

def 'BootstrapCluster returns a valid result'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def hostnames = ['test.io', 'test-2.io']

    when:
    def response = subject.bootstrapCluster(
      environmentNm,
      new BootstrapRequest(clusterName: clusterNm, hostnames: hostnames),
      new User()
    )

    then:
    1 * clusters.bootstrapCluster(environmentNm, clusterNm, hostnames)
    expect response, notNullValue()
    verifyAll(response) {
      expect status, equalTo(201)
      expect location, notNullValue()
      expect location.path, containsString("/environment/${environmentNm}/cluster/${clusterNm}")
    }
  }

def 'BootstrapCluster throws a InProgressException if a bootstrap is in progress for a cluster'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def hostnames = ['test.io', 'test-2.io']
    clusters.bootstrapCluster(environmentNm, clusterNm, hostnames) >> {throw new InProgressException()}

    when:
    subject.bootstrapCluster(
      environmentNm,
      new BootstrapRequest(clusterNm, hostnames),
      new User()
    )

    then:
    thrown InProgressException
  }

def 'BootstrapCluster throws a AlreadyExistsException if a cluster already exists'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def hostnames = ['test.io', 'test-2.io']
    clusters.bootstrapCluster(environmentNm, clusterNm, hostnames) >> {throw new AlreadyExistsException()}

    when:
    subject.bootstrapCluster(
      environmentNm,
      new BootstrapRequest(clusterNm, hostnames),
      new User()
    )

    then:
    thrown AlreadyExistsException
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

def 'DeleteCluster returns a valid result'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'

    when:
    def response = subject.deleteCluster(environmentNm, clusterNm, new User())

    then:
    1 * clusters.destroyCluster(environmentNm, clusterNm)
    expect response, notNullValue()
    verifyAll(response) {
      expect status, equalTo(202)
      expect location, notNullValue()
      expect location.path, containsString("/environment/${environmentNm}/cluster/${clusterNm}")
    }
  }

def 'DeleteCluster throws a NotFoundException if no cluster exists'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    clusters.destroyCluster(environmentNm, clusterNm) >> {throw new NotFoundException()}

    when:
    subject.deleteCluster(
      environmentNm,
      clusterNm,
      new User()
    )

    then:
    thrown NotFoundException
  }

def 'DeleteCluster throws a InProgressException if a cluster bootstrap is in progress'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    clusters.destroyCluster(environmentNm, clusterNm) >> {throw new InProgressException()}

    when:
    subject.deleteCluster(
      environmentNm,
      clusterNm,
      new User()
    )

    then:
    thrown InProgressException
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

def 'GetCluster returns a cluster'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def clusterConfig = 'cluster config yaml'
    def clusterNodes = [new ClusterNode(hostname:  'test.io', status: Status.IN_PROGRESS)]
    def cluster = new Cluster(
      envName: environmentNm,
      name: clusterNm,
      config: clusterConfig,
      nodes: clusterNodes
    )

    when:
    def result = subject.getCluster(environmentNm, clusterNm)

    then:
    1 * clusters.getCluster(environmentNm, clusterNm) >> cluster
    expect result, notNullValue()
    expect result, equalTo(cluster)
  }

def 'GetCluster throws a NotFoundException if no cluster exists'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    clusters.getCluster(environmentNm, clusterNm) >> {throw new NotFoundException()}

    when:
    subject.getCluster(
      environmentNm,
      clusterNm
    )

    then:
    thrown NotFoundException
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

def 'GetCustomerKubeconfig returns a valid result'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    def customerKubeconfig = new Kubeconfig('kubeconfig String')
    when:
    def result = subject.getCustomerKubeconfig(environmentNm, clusterNm)

    then:
    1 * clusters.getCustomerKubeconfig(environmentNm, clusterNm) >> customerKubeconfig
    expect result, notNullValue()
    expect result, equalTo(customerKubeconfig)
  }

def 'GetCustomerKubeconfig throws a NotFoundException if no cluster exists'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    clusters.getCustomerKubeconfig(environmentNm, clusterNm) >> {throw new NotFoundException()}

    when:
    subject.getCustomerKubeconfig(
      environmentNm,
      clusterNm
    )

    then:
    thrown NotFoundException
  }

def 'GetCustomerKubeconfig throws a InProgressException if the cluster bootstrap is still processing'() {
    given:
    def environmentNm = 'test-env'
    def clusterNm = 'test-cluster'
    clusters.getCustomerKubeconfig(environmentNm, clusterNm) >> {throw new InProgressException()}

    when:
    subject.getCustomerKubeconfig(
      environmentNm,
      clusterNm
    )

    then:
    thrown InProgressException
  }
}
