package io.cratekube.clustermgmt.dropwizard.auth

import groovy.transform.Immutable

import javax.validation.Valid
import javax.validation.constraints.NotEmpty

/**
 * Configuration object for the auth bundle.
 */
@Immutable
class ApiKeyAuthConfig {
  @Valid
  @NotEmpty
  List<ApiKey> apiKeys
}

/**
 * Represents an api key configuration object.
 */
@Immutable
class ApiKey {
  @NotEmpty
  String name

  @NotEmpty
  String key

  Set<String> roles = [] as Set<String>
}
