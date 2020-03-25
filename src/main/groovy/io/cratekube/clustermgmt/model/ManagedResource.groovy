package io.cratekube.clustermgmt.model

import groovy.transform.EqualsAndHashCode

/**
 * Represents any resource that can be deployed by Kubernetes.
 */
@EqualsAndHashCode
class ManagedResource {
  /** name of this resource. also used as base filename */
  String name
  /** resource file content */
  String config
  /** status of the managed resource */
  Status status
}
