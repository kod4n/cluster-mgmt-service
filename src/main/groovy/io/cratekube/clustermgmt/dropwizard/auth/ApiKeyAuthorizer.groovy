package io.cratekube.clustermgmt.dropwizard.auth

import io.dropwizard.auth.Authorizer

import javax.ws.rs.ForbiddenException

/**
 * The authorizer used for simple token based auth.
 * <p>
 * This authorizer delegates the authorization to the {@link User#checkUserInRole(String)} function.
 * If the user is in the role true will be returned, otherwise false will be returned.
 * </p>
 */
class ApiKeyAuthorizer implements Authorizer<User> {
  @Override
  boolean authorize(User user, String role) {
    try {
      user.checkUserInRole(role)
      return true
    } catch (ForbiddenException ignored) {
      return false
    }
  }
}
