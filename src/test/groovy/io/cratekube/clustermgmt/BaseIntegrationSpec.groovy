package io.cratekube.clustermgmt

import groovy.transform.Memoized
import io.cratekube.clustermgmt.api.ClusterApi
import io.cratekube.clustermgmt.api.ManagedResourcesApi
import org.spockframework.mock.MockUtil
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp
import spock.lang.Specification

import javax.inject.Inject
import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation

/**
 * Base class for all integration specs. This class provides a client for interacting with the
 * Dropwizard application's API.
 */
@UseDropwizardApp(value = App, hooks = IntegrationSpecHook, config = 'app.yml')
abstract class BaseIntegrationSpec extends Specification {
  MockUtil mockUtil = new MockUtil()
  @Inject Client client
  @Inject ClusterApi clusters
  @Inject ManagedResourcesApi resources
  @Inject AppConfig config

  def setup() {
    [clusters, resources].findAll { mockUtil.isMock(it) }
      .each { mockUtil.attachMock(it, this) }
  }

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

  @Memoized
  protected Invocation.Builder requestWithAdminToken(String path = '') {
    baseRequest(path)
      .header('Authorization', "Bearer ${config.auth.apiKeys.find {it.name == 'admin'}.key}")
  }
}
