package io.cratekube.clustermgmt.modules

import com.google.inject.Provides
import io.cratekube.clustermgmt.AppConfig
import io.cratekube.clustermgmt.api.ClusterApi
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import io.dropwizard.client.JerseyClientBuilder
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule
import spock.mock.DetachedMockFactory

import javax.inject.Singleton
import javax.ws.rs.client.Client

/**
 * Guice module used for integration specs.
 */
class IntegrationSpecModule extends DropwizardAwareModule<AppConfig> {
  DetachedMockFactory mockFactory = new DetachedMockFactory()

  @Override
  protected void configure() {
    bind ClusterApi toInstance mock(ClusterApi)
    bind ManagedResourcesApi toInstance mock(ManagedResourcesApi)
  }

  def <T> T mock(Class<T> type) {
    return mockFactory.Mock(type)
  }

  @Provides
  @Singleton
  Client clientProvider() {
    return new JerseyClientBuilder(environment()).using(configuration().jerseyClient).build('external-client')
  }
}
