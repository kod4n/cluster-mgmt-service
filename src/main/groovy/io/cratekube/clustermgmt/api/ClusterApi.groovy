package io.cratekube.clustermgmt.api

import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.model.Cluster
import io.cratekube.clustermgmt.model.Kubeconfig

/**
 * Base API for interactions with clusters.
 */
interface ClusterApi {
  /**
   * Bootstraps a cluster.
   * <p>If the cluster has already been created a {@link AlreadyExistsException} will be thrown.</p>
   * <p>If the cluster is being bootstrapped or deleted a {@link InProgressException} will be thrown.</p>
   *
   * @param envName {@code non-empty} environment name
   * @param clusterName {@code non-empty} cluster name
   * @param hostnames {@code non-empty} list of host FQDNs
   * @throws AlreadyExistsException
   * @throws InProgressException
   */
  void bootstrapCluster(String envName, String clusterName, List<String> hostnames)
    throws AlreadyExistsException, InProgressException

  /**
   * Deletes a cluster.
   * <p>If no cluster exists a {@link NotFoundException} will be thrown.</p>
   * <p>If the cluster is being bootstrapped or deleted a {@link InProgressException} will be thrown.</p>
   *
   * @param envName {@code non-empty} name of the environment cluster will live in
   * @param clusterName {@code non-empty} name of cluster
   * @throws InProgressException
   * @throws NotFoundException
   */
  void destroyCluster(String envName, String clusterName)
    throws InProgressException, NotFoundException

  /**
   * Retrieves a cluster.
   * <p>If the cluster is not found a {@link NotFoundException} will be thrown.</p>
   *
   * @param envName {@code non-empty} name of the environment cluster lives in
   * @param clusterName {@code non-empty} name of cluster
   * @return cluster object
   * @throws InProgressException
   * @throws NotFoundException
   */
  Cluster getCluster(String envName, String clusterName)
    throws NotFoundException

  /**
   * Retrieves a customers kubeconfig.
   * <p>This additional kubeconfig should be created directly after cluster
   * bootstrap to distribute for access to the customer cluster.</p>
   * <p>If the cluster or customer kubeconfig is not found a {@link NotFoundException} will be thrown.</p>
   * <p>If the cluster is being bootstrapped or deleted a {@link InProgressException} will be thrown.</p>
   *
   * @param envName {@code non-empty} name of the environment cluster lives in
   * @param clusterName {@code non-empty} name of cluster
   * @return the customers kubeconfig
   * @throws NotFoundException
   * @throws InProgressException
   */
  Kubeconfig getCustomerKubeconfig(String envName, String clusterName)
    throws NotFoundException, InProgressException
}
