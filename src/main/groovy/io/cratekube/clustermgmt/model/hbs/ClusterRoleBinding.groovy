package io.cratekube.clustermgmt.model.hbs

import groovy.transform.Immutable

/**
 * Model populated and bound with {@code clusterrolebindings/serviceaccount:cluster-admin.yaml.hbs}.
 */
@Immutable
class ClusterRoleBinding {
  /** service account name */
  String serviceAccountName
}
