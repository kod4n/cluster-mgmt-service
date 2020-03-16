package io.cratekube.clustermgmt.dropwizard.auth

import io.dropwizard.Configuration
import io.dropwizard.ConfiguredBundle
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.setup.Environment
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature

/**
 * Bundle used for api key authc/authz.
 *
 * @param <T> the configuration type
 */

abstract class ApiKeyAuthBundle<T extends Configuration> implements ConfiguredBundle<T> {
  /**
   * Configures the jersey environment with an OAuth filter that uses a bearer token authentication method.
   *
   * @param configuration the Dropwizard configuration object
   * @param environment the Dropwizard environment object
   */
  @Override
  void run(T configuration, Environment environment) throws Exception {
    def tokenFilter = new OAuthCredentialAuthFilter.Builder().with {
      authenticator = new ApiKeyAuthenticator(getApiKeyAuthConfig(configuration).apiKeys)
      authorizer = new ApiKeyAuthorizer()
      prefix = 'Bearer'
      buildAuthFilter()
    }
    environment.jersey().with {
      register new AuthDynamicFeature(tokenFilter)
      register RolesAllowedDynamicFeature
      register new AuthValueFactoryProvider.Binder(User)
    }
  }

  /**
   * Used to provide the bundle with the ApiKeyAuthConfig
   *
   * @param configuration the Dropwizard configuration object
   * @return the ApiKeyAuthConfig
   */
  protected abstract ApiKeyAuthConfig getApiKeyAuthConfig(T configuration)
}
