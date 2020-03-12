package io.cratekube.clustermgmt.model

import groovy.transform.Immutable

/**
 * Customers kubeconfig for a cluster.
 */
@Immutable
class Kubeconfig {
  /** kubeconfig file contents */
  String kubeconfig
}
