package io.cratekube.clustermgmt.model

/**
 * Represents properties and configuration for a cluster.
 */
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

class ClusterNode {
  /** hostname of the node */
  String hostname
  /** status of the node */
  Status status
}
