package io.cratekube.clustermgmt.model

import groovy.transform.Immutable

/**
 * Wrapper of kubeconfig for a cluster.
 */
@Immutable
class Kubeconfig {
  /** kubeconfig file contents */
  String kubeconfig
}
