package io.cratekube.clustermgmt.model.hbs

import groovy.transform.Immutable

/**
 * Model populated and bound with {@code customer-kubeconfig.yaml.hbs}.
 */
@Immutable
class CustomerKubeconfig {
  /** service account name */
  String serviceAccountName
  /** token info */
  String token
  /** certificate-authority-data info */
  String certificateAuthorityData
  /** server info */
  String server
}
