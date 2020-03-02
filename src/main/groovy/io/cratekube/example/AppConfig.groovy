package io.cratekube.example

import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration

import javax.validation.Valid
import javax.validation.constraints.NotNull

/**
 * Configuration class for this Dropwizard application.
 */
class AppConfig extends Configuration {
  JerseyClientConfiguration jerseyClient

  @Valid
  @NotNull
  SwaggerBundleConfiguration swagger
}
