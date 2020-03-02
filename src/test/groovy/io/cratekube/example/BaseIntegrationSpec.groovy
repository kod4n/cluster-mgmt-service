package io.cratekube.example

import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification

import javax.inject.Inject
import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation

/**
 * Base class for all integration specs.  This class provides a client for interacting with the
 * Dropwizard application's API.
 */
@UseDropwizardApp(value = App, hooks = IntegrationSpecHook, config = 'src/test/resources/testapp.yml')
class BaseIntegrationSpec extends Specification {
  @Inject Client client

  /**
   * Creates a client invocation builder using the provided path.
   *
   * @param path {@code non-null} api path to call
   * @return an {@link Invocation.Builder} instance for the request
   */
  Invocation.Builder baseRequest(String path = '') {
    return client.target("http://localhost:9000${path}").request()
  }
}
