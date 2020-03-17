package io.cratekube.clustermgmt.model.hbs

import groovy.transform.Immutable

/**
 * Model populated and bound with {@code serviceaccounts/service-account.yaml.hbs}.
 */
@Immutable
class ServiceAccount {
  /** service account name */
  String name
  /** service account namespace */
  String namespaceName
}
