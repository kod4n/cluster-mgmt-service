package io.cratekube.clustermgmt.modules

import com.google.inject.Provides
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ClusterApi
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.model.Cluster
import io.cratekube.clustermgmt.model.ManagedResource
import io.cratekube.clustermgmt.modules.annotation.ClusterCache
import io.cratekube.clustermgmt.modules.annotation.ClusterProcessExecutor
import io.cratekube.clustermgmt.modules.annotation.ManagedResourceExecutor
import io.cratekube.clustermgmt.modules.annotation.ManagedResourceCache
import io.cratekube.clustermgmt.modules.annotation.ClusterExecutor
import io.cratekube.clustermgmt.modules.annotation.ManagedResourceProcessExecutor
import io.cratekube.clustermgmt.service.ClusterService
import io.cratekube.clustermgmt.service.KubectlCommand
import io.cratekube.clustermgmt.service.ManagedResourcesService
import io.cratekube.clustermgmt.service.RkeCommand
import org.apache.commons.vfs2.FileSystemManager
import org.apache.commons.vfs2.VFS

import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule

import javax.inject.Singleton
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import org.apache.commons.collections4.map.LRUMap

/**
 * Default module to be used when running this application.
 */
class ProductionModule extends DropwizardAwareModule<AppConfig> {
  @Override
  protected void configure() {
    bind ClusterApi to ClusterService
    bind FileSystemManager toInstance VFS.manager
    bind ProcessExecutor annotatedWith ClusterProcessExecutor to RkeCommand
    bind ProcessExecutor annotatedWith ManagedResourceProcessExecutor to KubectlCommand
    bind ManagedResourcesApi to ManagedResourcesService
    bind AppConfig toInstance configuration()
  }

  @Provides
  @ClusterCache
  @Singleton
  Map<String, Cluster> clusterBootstrapCache() {
    return new LRUMap<String, Cluster>()
  }

  @Provides
  @ManagedResourceCache
  @Singleton
  Map<String, List<ManagedResource>> managedServiceCache() {
    return new LRUMap<String, List<ManagedResource>>()
  }

  @Provides
  @ClusterExecutor
  @Singleton
  Executor clusterExecutor() {
    return Executors.newCachedThreadPool()
  }

  @Provides
  @ManagedResourceExecutor
  @Singleton
  Executor managedServiceExecutor() {
    return Executors.newCachedThreadPool()
  }
}
