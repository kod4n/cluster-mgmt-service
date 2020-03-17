package io.cratekube.clustermgmt.dropwizard.auth

import org.valid4j.errors.RequireViolation
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static org.hamcrest.Matchers.equalTo
import static spock.util.matcher.HamcrestSupport.expect

class ApiKeyAuthenticatorSpec extends Specification {
  @Subject ApiKeyAuthenticator subject
  List<ApiKey> apiKeys

  def setup() {
    apiKeys = [
      new ApiKey(name: 'admin', key: 'admintoken', roles: ['admin'])
    ]
    subject = new ApiKeyAuthenticator(apiKeys)
  }

  @Unroll
  def 'ApiKeyAuthenticator should require valid params'() {
    when:
    new ApiKeyAuthenticator(apiKeyz)

    then:
    thrown RequireViolation

    where:
    apiKeyz << [null, [null]]
  }

  def 'Authenticate returns user specified by token'() {
    when:
    def result = subject.authenticate('admintoken')

    then:
    expect result.isPresent(), equalTo(true)
    verifyAll(result.get()) {
      expect name, equalTo('admin')
      expect roles, equalTo(['admin'] as Set)
    }
  }
}
