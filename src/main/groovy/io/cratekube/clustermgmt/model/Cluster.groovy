package io.cratekube.clustermgmt.model

import groovy.transform.Immutable

/**
 * Represents properties and configuration for a cluster.
 */
@Immutable
class Cluster {
  /** environment name */
  String envName
  /** cluster name */
  String name
  /** content of cluster.yml */
  String config
  /** list of the clusters hosts with status */
  List<ClusterNode> nodes
}

@Immutable
class ClusterNode {
  /** hostname of the node */
  String hostname
  /** status of the node */
  Status status
}
