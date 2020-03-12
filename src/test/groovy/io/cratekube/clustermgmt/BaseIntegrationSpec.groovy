package io.cratekube.clustermgmt

import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification

import javax.inject.Inject
import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation

/**
 * Base class for all integration specs. This class provides a client for interacting with the
 * Dropwizard application's API.
 */
@UseDropwizardApp(value = App, hooks = IntegrationSpecHook, config = 'src/test/resources/testapp.yml')
abstract class BaseIntegrationSpec extends Specification {
  @Inject Client client

  /**
   * Base path used for API requests. Can be overridden by classes extending this spec.
   *
   * @return the base API path for requests
   */
  abstract String getBaseRequestPath()

  /**
   * Creates a client invocation builder using the provided path.
   *
   * @param path {@code non-null} api path to call
   * @return an {@link Invocation.Builder} instance for the request
   */
  protected Invocation.Builder baseRequest(String path = '') {
    return client.target("http://localhost:9000${baseRequestPath}${path}").request()
  }
}
