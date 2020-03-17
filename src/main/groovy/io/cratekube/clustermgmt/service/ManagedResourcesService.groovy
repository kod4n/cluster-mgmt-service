package io.cratekube.clustermgmt.service

import groovy.util.logging.Slf4j
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.api.exception.AlreadyExistsException
import io.cratekube.clustermgmt.api.exception.InProgressException
import io.cratekube.clustermgmt.api.exception.NotFoundException
import io.cratekube.clustermgmt.model.ManagedResource
import io.cratekube.clustermgmt.modules.annotation.ManagedResourceCache
import io.cratekube.clustermgmt.modules.annotation.ManagedResourceExecutor
import io.cratekube.clustermgmt.modules.annotation.ManagedResourceProcessExecutor
import org.apache.commons.vfs2.FileSystemManager

import javax.inject.Inject
import java.util.concurrent.Executor

import static org.hamcrest.core.IsNull.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

/**
 * Default implementation fo the {@link ManagedResourcesApi} interface.
 *
 * @see ManagedResourcesApi
 */
@Slf4j
class ManagedResourcesService implements ManagedResourcesApi {
  FileSystemManager fs
  ProcessExecutor kubectl
  AppConfig config

  // for async operations
  Executor executor
  /** map of envName/clusterName to their managed resources. */
  Map<String, List<ManagedResource>> managedResourceCache

  @Inject
  ManagedResourcesService(FileSystemManager fs, @ManagedResourceProcessExecutor ProcessExecutor kubectl, AppConfig config,
                          @ManagedResourceExecutor Executor executor, @ManagedResourceCache Map<String, List<ManagedResource>> managedResourceCache) {
    this.fs = require fs, notNullValue()
    this.kubectl = require kubectl, notNullValue()
    this.config = require config, notNullValue()
    this.executor = require executor, notNullValue()
    this.managedResourceCache = require managedResourceCache, notNullValue()
  }

  @Override
  void bootstrapManagedResources(String envName, String clusterName)
    throws NotFoundException, AlreadyExistsException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()

    /**
     * This is called right after RKE creates the cluster.
     *
     * <p>We expect this to be invoked from a background thread and it will be a blocking call.</p>
     *
     * <p>We will need to create a customer service account and bind the service account to the
     * cluster-admin role with a clusterrolebinding. There are templates in the resource directory that should be used.</p>
     *
     * <p>A namespace will need created (see the service account template) and a customer kubeconfig will need created
     * otherwise the customer won't be able to access their cluster.
     * How to create a separate customer kubeconfig:  http://docs.shippable.com/deploy/tutorial/create-kubeconfig-for-self-hosted-kubernetes-cluster</p>
     *
     */
  }

  @Override
  void deployManagedResource(String envName, String clusterName, ManagedResource resource)
    throws NotFoundException, InProgressException, AlreadyExistsException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require resource, notNullValue()
  }

  @Override
  void removeManagedResource(String envName, String clusterName, String resourceName)
    throws NotFoundException, InProgressException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require resourceName, notEmptyString()
  }

  @Override
  ManagedResource getManagedResource(String envName, String clusterName, String resourceName)
    throws NotFoundException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    require resourceName, notEmptyString()
    return null
  }

  @Override
  List<ManagedResource> getManagedResources(String envName, String clusterName)
    throws NotFoundException {
    require envName, notEmptyString()
    require clusterName, notEmptyString()
    return []
  }
}
