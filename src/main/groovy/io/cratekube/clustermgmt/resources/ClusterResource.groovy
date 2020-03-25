package io.cratekube.clustermgmt.resources

import groovy.util.logging.Slf4j
import io.cratekube.clustermgmt.api.ClusterApi
import io.cratekube.clustermgmt.dropwizard.auth.User
import io.cratekube.clustermgmt.model.Cluster
import io.cratekube.clustermgmt.model.Kubeconfig
import io.cratekube.clustermgmt.resources.request.BootstrapRequest
import io.dropwizard.auth.Auth
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam

import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

import static javax.ws.rs.core.Response.accepted
import static javax.ws.rs.core.Response.created
import static org.hamcrest.core.IsNull.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

@Api
@Path('environment/{envName}/cluster')
@Produces('application/json')
@Consumes('application/json')
@Slf4j
class ClusterResource {
  ClusterApi clusters

  @Inject
  ClusterResource(ClusterApi clusters) {
    this.clusters = require clusters, notNullValue()
  }

  /**
   * Bootstraps a cluster.
   * <p>If no prior bootstraps for the cluster are in progress a cluster bootstrap will be initiated and a 201 response will be returned with the location header set to the
   * status resource.</p>
   * <p>If a cluster bootstrap is in progress or already exists a 409 response will be returned.</p>
   *
   * @param envName {@code non-empty} environment name
   * @param bootstrapRequest {@code non-null} bootstrap request for cluster
   * @return 201 response and set location header when a cluster creation is initiated, 409 response if a request is in progress, a cluster already exists or if the requested zone is not supported
   * @throws io.cratekube.clustermgmt.api.exception.InProgressException if a cluster bootstrap is in progress for a {@link BootstrapRequest#clusterName}
   * @throws io.cratekube.clustermgmt.api.exception.AlreadyExistsException if a cluster already exists
   */
  @POST
  @RolesAllowed('admin')
  Response bootstrapCluster(
    @PathParam('envName') String envName,
    @Valid BootstrapRequest bootstrapRequest,
    @ApiParam(hidden = true) @Auth User user
  ) {
    require envName, notEmptyString()
    require bootstrapRequest, notNullValue()
    require user, notNullValue()

    log.debug 'action [bootstrap-cluster]. Environment [{}]. BootstrapRequest=[{}]', envName, bootstrapRequest

    return bootstrapRequest.with {
      clusters.bootstrapCluster(envName, clusterName, hostnames)
      created("/environment/${envName}/cluster/${clusterName}".toURI()).build()
    }
  }

  /**
   * Deletes a cluster.
   * <p>If no bootstraps for the cluster are in
   * progress a cluster deletion will be initiated and a 202 response will be returned.</p>
   * <p>If a cluster bootstrap is in progress a 409 response will be returned.</p>
   * <p>If the cluster does not exist a 404 response will be returned.</p>
   *
   * @param envName {@code non-empty} environment name
   * @param clusterName {@code non-null} cluster name
   * @return 202 accepted response and set location header when a cluster deletion is initiated
   * @throws io.cratekube.clustermgmt.api.exception.NotFoundException if no cluster exists
   * @throws io.cratekube.clustermgmt.api.exception.InProgressException if a cluster bootstrap is in progress
   */
  @DELETE
  @RolesAllowed('admin')
  @Path('{clusterName}')
  Response deleteCluster(
    @PathParam('envName') String envName,
    @PathParam('clusterName') String clusterName,
    @ApiParam(hidden = true) @Auth User user
  ) {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require user, notNullValue()

    log.debug 'action [delete-cluster]. Environment [{}] Cluster [{}]', envName, clusterName

    clusters.destroyCluster(envName, clusterName)
    return accepted().location("/environment/${envName}/cluster/${clusterName}".toURI()).build()
  }

  /**
   * Returns the current configuration for the cluster.
   * <p>If the cluster cannot be found a 404 NOT FOUND response will be returned.</p>
   *
   * @param envName {@code non-empty} environment name
   * @param clusterName {@code non-null} cluster name
   * @return cluster configuration
   * @throws io.cratekube.clustermgmt.api.exception.NotFoundException if no cluster exists
   */
  @GET
  @Path('{clusterName}')
  Cluster getCluster(
    @PathParam('envName') String envName,
    @PathParam('clusterName') String clusterName
  ) {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    log.debug 'action [get-cluster]. Environment [{}] Cluster [{}]', envName, clusterName

    return clusters.getCluster(envName, clusterName)
  }

  /**
   * Returns the customer kubeconfig for a cluster.
   * <p>If the bootstrap is still in progress a 409 CONFLICT response will be returned.</p>
   * <p>If the cluster cannot be found a 404 NOT FOUND response will be returned.</p>
   *
   * @param envName {@code non-empty} environment name
   * @param clusterName {@code non-null} cluster name
   * @return customer kubeconfig
   * @throws io.cratekube.clustermgmt.api.exception.NotFoundException if no cluster exists
   * @throws io.cratekube.clustermgmt.api.exception.InProgressException if the bootstrap is still processing
   */
  @GET
  @Path('{clusterName}/kubeconfig/customer')
  Kubeconfig getCustomerKubeconfig(
    @PathParam('envName') String envName,
    @PathParam('clusterName') String clusterName
  ) {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    log.debug 'action [get-kubeconfig]. Environment [{}] Cluster [{}]', envName, clusterName
    return clusters.getCustomerKubeconfig(envName, clusterName)
  }
}
