package io.cratekube.clustermgmt.resources

import groovy.util.logging.Slf4j
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.model.ManagedResource
import io.swagger.annotations.Api

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

import static org.hamcrest.core.IsNull.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

@Api
@Path('environment/{envName}/cluster/{clusterName}/resource')
@Produces('application/json')
@Consumes('application/json')
@Slf4j
class ManagedResourcesResource {
  ManagedResourcesApi resources

  @Inject
  ManagedResourcesResource(ManagedResourcesApi resources) {
    this.resources = require resources, notNullValue()
  }
  /**
   * Deploys a managed resource.
   * <p>If the requested managed resource for the cluster is being created a 201 response
   * will be returned with the location header set to the resource.</p>
   * <p>If the cluster does not exist a 404 response will be returned.</p>
   * <p>If a managed resource is being created or already exists a 409 response will be returned.</p>
   *
   * @param envName {@code non-empty} environment name
   * @param clusterName {@code non-empty} cluster name
   * @param req {@code non-null} managed resource request for the cluster
   * @return 201 response and set location header when a cluster creation is initiated, a 404 if the cluster does not exist or a 409 response if the managed resource exists already or creation is in progress
   * @throws io.cratekube.clustermgmt.api.exception.InProgressException if the managed resource creation is in progress
   * @throws io.cratekube.clustermgmt.api.exception.AlreadyExistsException if the managed resource already exists
   * @throws io.cratekube.clustermgmt.api.exception.NotFoundException if a cluster does not exist
   */
  @POST
  Response createManagedResource(
    @PathParam('envName') String envName,
    @PathParam('clusterName') String clusterName,
    @Valid ManagedResource req
  ) {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require req, notNullValue()

    log.debug 'action [create-managed-resource]'
    return null
  }

  /**
   * Deletes a managed resource.
   * <p>If the requested managed resource for the cluster is being deleted a 202 response will be returned.</p>
   *
   * @param envName {@code non-empty} environment name
   * @param clusterName {@code non-empty} cluster name
   * @param resourceName {@code non-empty} managed resource name
   * @return a 202 response and set location header when a managed resource deletion is initiated, a 404 if the cluster or managed resource does not exist or a 409 response if the managed resource creation is in progress
   * @throws io.cratekube.clustermgmt.api.exception.InProgressException if the managed resource creation is in progress
   * @throws io.cratekube.clustermgmt.api.exception.NotFoundException if managed resource does not exist
   * @throws io.cratekube.clustermgmt.api.exception.NotFoundException if a cluster does not exist
   */
  @DELETE
  @Path('{resourceName}')
  Response deleteManagedResource(
    @PathParam('envName') String envName,
    @PathParam('clusterName') String clusterName,
    @PathParam('resourceName') String resourceName
  ) {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require resourceName, notEmptyString()

    log.debug 'action [delete-managed-resource]'
    return null
  }

  /**
   * Returns the managed resource with state.
   * <p>If the cluster or resource cannot be found a 404 NOT FOUND response will be returned.</p>
   *
   * @param envName {@code non-empty} environment name
   * @param clusterName {@code non-empty} cluster name
   * @param resourceName {@code non-empty} managed resource name
   * @return the managed resource
   * @throws io.cratekube.clustermgmt.api.exception.NotFoundException if no cluster exists
   * @throws io.cratekube.clustermgmt.api.exception.NotFoundException if managed resource does not exist
   */
  @GET
  @Path('{resourceName}')
  ManagedResource getManagedResource(
    @PathParam('envName') String envName,
    @PathParam('clusterName') String clusterName,
    @PathParam('resourceName') String resourceName
  ) {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require resourceName, notEmptyString()

    log.debug 'action [get-managed-resource]'
    return null
  }

  /**
   * Returns the status for all managed resources in a cluster.
   * <p>If the cluster cannot be found a 404 NOT FOUND response will be returned.</p>
   *
   * @param envName {@code non-empty} environment name
   * @param clusterName {@code not-empty} cluster name
   * @return all managed resources for a cluster in an environment
   * @throws io.cratekube.clustermgmt.api.exception.NotFoundException if no cluster exists
   */
  @GET
  List<ManagedResource> getManagedResources(
    @PathParam('envName') String envName,
    @PathParam('clusterName') String clusterName
  ) {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    log.debug 'action [get-managed-resources]'
    return []
  }

}
