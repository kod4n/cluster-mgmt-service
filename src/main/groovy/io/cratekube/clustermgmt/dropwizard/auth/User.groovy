package io.cratekube.clustermgmt.dropwizard.auth

import javax.ws.rs.ForbiddenException
import java.security.Principal

/**
 * Default user principal to use for authentication.
 */
class User implements Principal {
  /**
   * Username for the operating user.
   */
  String name

  /**
   * Unique set of roles this user has been granted.
   */
  Set<String> roles = [] as Set<String>

  /**
   * Determines if a user has a given role.  If the user does not have the role a {@link javax.ws.rs.ForbiddenException} will be
   * thrown, results in a 403 error response.
   *
   * @param role the role to check
   *
   * @throws javax.ws.rs.ForbiddenException
   */
  void checkUserInRole(String role) {
    if (!roles.contains(role)) {
      throw new ForbiddenException()
    }
  }
}
