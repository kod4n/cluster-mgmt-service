package io.cratekube.clustermgmt.config

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

/**
 * Configuration object for the RKE command line binary.
 */
class RkeConfig {
  @NotEmpty
  String nodeUser

  @NotEmpty
  String sshKeyPath

  @NotNull
  Boolean podSecurityPolicyEnabled
}
