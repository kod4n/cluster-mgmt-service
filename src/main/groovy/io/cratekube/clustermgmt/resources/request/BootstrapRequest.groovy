package io.cratekube.clustermgmt.resources.request

import groovy.transform.Immutable

import javax.validation.constraints.NotEmpty

/**
 * Request object used for bootstrapping new clusters.
 */
@Immutable
class BootstrapRequest {
  /** requested name for cluster */
  @NotEmpty
  String clusterName
  /** list of FQDNs for all the nodes that should be included in a cluster */
  @NotEmpty
  List<String> hostnames
}
