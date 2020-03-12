package io.cratekube.clustermgmt.config

import javax.validation.constraints.NotEmpty

/**
 * Configuration object for the RKE command line binary.
 */
class RkeConfig {
  @NotEmpty
  String nodeUser

  @NotEmpty
  String sshKeyPath
}
