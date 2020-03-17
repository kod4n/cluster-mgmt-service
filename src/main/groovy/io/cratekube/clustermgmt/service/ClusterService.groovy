package io.cratekube.clustermgmt.service

import groovy.util.logging.Slf4j
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ClusterApi
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.model.Cluster
import io.cratekube.clustermgmt.model.Kubeconfig
import io.cratekube.clustermgmt.modules.annotation.ClusterCache
import io.cratekube.clustermgmt.modules.annotation.ClusterExecutor
import io.cratekube.clustermgmt.modules.annotation.ClusterProcessExecutor
import org.apache.commons.vfs2.FileSystemManager

import javax.inject.Inject
import java.util.concurrent.Executor

import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.not
import static org.hamcrest.core.Every.everyItem
import static org.hamcrest.core.IsNull.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

/**
 * Default implementation for the {@link ClusterApi} interface.  Provides access to operation on clusters.
 *
 * @see ClusterApi
 */
@Slf4j
class ClusterService implements ClusterApi {
  FileSystemManager fs
  ProcessExecutor rke
  ManagedResourcesApi resources
  AppConfig config

  // for async operations
  Executor executor
  /** map of envName/clusterName to cluster details */
  Map<String, Cluster> clusterCache

  @Inject
  ClusterService(FileSystemManager fs, @ClusterProcessExecutor ProcessExecutor rke, ManagedResourcesApi resources,
                 AppConfig config, @ClusterExecutor Executor executor, @ClusterCache Map<String, Cluster> clusterCache) {
    this.fs = require fs, notNullValue()
    this.rke = require rke, notNullValue()
    this.resources = require resources, notNullValue()
    this.config = require config, notNullValue()
    this.executor = require executor, notNullValue()
    this.clusterCache = require clusterCache, notNullValue()
  }

  @Override
  void bootstrapCluster(String envName, String clusterName, List<String> hostnames)
    throws AlreadyExistsException, InProgressException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require hostnames, allOf(notNullValue(), not(empty()), everyItem(notEmptyString()))

    // call getCluster to look for an existing cluster status object in the in-memory LRU cache and throw InProgressException if it is still processing
    // and throw AlreadyExistsException if the desired cluster already exists

    // put initial cluster status in the cluster queue. queueKey = "${envName}/${name}"

    // background execution through executor
    // generate list of nodes for the rke-cluster-config template

    // create cluster config by applying  template in the resources directory

    // config file should be saved
    // /environment/{envName}/cluster/{name}/cluster.yml

    // run `rke up` using the generated config file in the cluster name directory and update the node statuses

    // deploy addition k8s resources required for configuring cluster using the ManagedResourcesApi
  }

  @Override
  void destroyCluster(String envName, String clusterName)
    throws InProgressException, NotFoundException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    // call getCluster to look for an existing cluster object in the in-memory LRU cache and throw InProgressException if it is still processing
    // and throw NotFoundException if the desired cluster does not exist
    // config file should be saved
    // /environment/{envName}/cluster/{clustName}/cluster.yml

    // run `rke delete` using the  config file in the cluster clustName directory and remove item from cluster queue if it exists
  }

  @Override
  Cluster getCluster(String envName, String clusterName)
    throws NotFoundException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    // look for an existing cluster status object in the in-memory LRU cache and return it if it exists

    // if no still processing cache value exists search for a config file created by rke at DEFAULT_CONFIG_LOCATION/environment/{envName}/cluster/{cluster}/kube_config_cluster.yml
    // build cluster object if file is found.

    // otherwise throw NotFoundException
    return null
  }

  @Override
  Kubeconfig getCustomerKubeconfig(String envName, String clusterName)
    throws NotFoundException, InProgressException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    // look for an existing cluster object in the in-memory LRU cache and throw InProgressException if it is still processing

    // if no still processing cache value exists search for a config file created by rke at DEFAULT_CONFIG_LOCATION/environment/{envName}/cluster/{cluster}/{customer-kubeconfig-name}.yml
    // build cluster object if file is found.

    // otherwise throw NotFoundException
    return null
  }
}
