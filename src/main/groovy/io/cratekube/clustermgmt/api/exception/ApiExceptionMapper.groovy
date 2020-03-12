package io.cratekube.clustermgmt.api.exception

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

/**
 * Exception mapper for {@link ApiException} errors.
 * <p>Responses will be built using the status code and message
 * available on the exception object.</p>
 */
@Provider
class ApiExceptionMapper implements ExceptionMapper<ApiException> {
  @Override
  Response toResponse(ApiException exception) {
    return Response.status(exception.errorCode).entity(message: exception.message).build()
  }
}
