package io.cratekube.clustermgmt.api

import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.model.ManagedResource

/**
 * Base interface for interactions with managed resources.
 */
interface ManagedResourcesApi {
  /**
   * Bootstraps a cluster with all required managed resources and configurations.
   * <p>If the cluster is not found a {@link NotFoundException} will be thrown.</p>
   *
   * @param envName {@code non-empty} name of the environment cluster lives in
   * @param clusterName {@code non-empty} name of cluster where the resources are deployed
   * @throws NotFoundException
   * @throws AlreadyExistsException
   */
  void bootstrapManagedResources(String envName, String clusterName)
    throws NotFoundException, AlreadyExistsException
  /**
   * Deploys a resource to an existing cluster.
   * <p>If the cluster is not found a {@link NotFoundException} will be thrown.</p>
   * <p>If the managed resource is being created or deleted a {@link InProgressException} will be thrown.</p>
   * <p>If the managed resource already exists a {@link AlreadyExistsException} will be thrown.</p>
   *
   * @param envName {@code non-empty} name of the environment cluster lives in
   * @param clusterName {@code non-empty} name of cluster where the resource should be deployed
   * @param resource {@code non-null} resource to deploy
   * @throws NotFoundException
   * @throws InProgressException
   * @throws AlreadyExistsException
   */
  void deployManagedResource(String envName, String clusterName, ManagedResource resource)
    throws NotFoundException, InProgressException, AlreadyExistsException

  /**
   * Removes a managed resource from an existing cluster.
   * <p>If the cluster is not found a {@link NotFoundException} will be thrown.</p>
   * <p>If the managed resource is being created or deleted a {@link InProgressException} will be thrown.</p>
   *
   * @param envName {@code non-empty} name of the environment cluster lives in
   * @param clusterName {@code non-empty} name of cluster where the resource is deployed
   * @param resourceName {@code non-empty} name of resource to remove
   * @throws NotFoundException
   * @throws InProgressException
   */
  void removeManagedResource(String envName, String clusterName, String resourceName)
    throws NotFoundException, InProgressException

  /**
   * Retrieves a managed resource from an existing cluster.
   * <p>If the cluster is not found a {@link NotFoundException} will be thrown.</p>
   *
   * @param envName {@code non-empty} name of the environment cluster lives in
   * @param clusterName {@code non-empty} name of cluster where the resource is deployed
   * @param resourceName {@code non-empty} name of resource to remove
   * @return the managed resource or null if no resource exists
   * @throws NotFoundException
   */
  ManagedResource getManagedResource(String envName, String clusterName, String resourceName)
    throws NotFoundException

  /**
   * Retrieves all manage resource for a cluster.
   * <p>If the cluster is not found a {@link NotFoundException} will be thrown.</p>
   *
   * @param envName {@code non-empty} name of the environment cluster lives in
   * @param clusterName {@code non-empty} name of cluster where the resource is deployed
   * @return all managed resource for the cluster of an empty list
   * @throws NotFoundException
   */
  List<ManagedResource> getManagedResources(String envName, String clusterName)
    throws NotFoundException
}
