package io.cratekube.clustermgmt.dropwizard.auth

import io.dropwizard.auth.AuthenticationException
import io.dropwizard.auth.Authenticator

import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.core.Every.everyItem
import static org.valid4j.Assertive.require

/**
 * The authenticator used for simple token based auth.
 * <p>
 * A user will be populated for the authenticate call when
 * the bearer token matches a token provided.
 * </p>
 */
class ApiKeyAuthenticator implements Authenticator<String, User> {
  List<ApiKey> apiKeys

  ApiKeyAuthenticator(List<ApiKey> apiKeys) {
      this.apiKeys = require apiKeys, allOf(notNullValue(), everyItem(notNullValue()))
  }

  @Override
  Optional<User> authenticate(String token) throws AuthenticationException {
    def apiKey = apiKeys.find { it.key == token }
    return Optional.ofNullable(apiKey ? new User(name: apiKey.name, roles: apiKey.roles) : null)
  }
}
