package io.cratekube.clustermgmt.api.exception

import groovy.transform.InheritConstructors

/**
 * Base exception for any error in a Crate API.
 */
@InheritConstructors
class ApiException extends RuntimeException {
  int errorCode = 500
}

@InheritConstructors
class NotFoundException extends ApiException {
  int errorCode = 404
}

@InheritConstructors
class AlreadyExistsException extends ApiException {
  int errorCode = 409
}

@InheritConstructors
class InProgressException extends ApiException {
  int errorCode = 409
}

@InheritConstructors
class FailedException extends ApiException { }

