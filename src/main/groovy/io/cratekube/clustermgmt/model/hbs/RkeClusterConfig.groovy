package io.cratekube.clustermgmt.model.hbs

import groovy.transform.Immutable

/**
 * Model populated and bound with {@code rke-cluster-config.hbs}.
 */
@Immutable
class RkeClusterConfig {
  /** path to SSH key used by RKE to access a node */
  String sshKeyPath
  /** whether or not pod security policy is enabled */
  Boolean podSecurityPolicyEnabled
  /** config for cluster nodes */
  List<RkeClusterNode> rkeClusterNodes
}

@Immutable
class RkeClusterNode {
  /** fully qualified domain name for a node. */
  String address
  /** account that will receive administrator access to nodes */
  String user
  /** the node will be used for, e.g.: controlplane, etcd, worker, etc... */
  List<String> roles
}
