package io.cratekube.clustermgmt

import io.cratekube.clustermgmt.config.RkeConfig
import io.cratekube.clustermgmt.dropwizard.auth.ApiKeyAuthConfig
import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration

import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

/**
 * Configuration class for this Dropwizard application.
 */
class AppConfig extends Configuration {
  JerseyClientConfiguration jerseyClient

  @Valid
  @NotNull
  SwaggerBundleConfiguration swagger

  @Valid
  @NotNull
  RkeConfig rke

  @NotEmpty
  String configLocation

  @Valid
  @NotNull
  ApiKeyAuthConfig auth
}
