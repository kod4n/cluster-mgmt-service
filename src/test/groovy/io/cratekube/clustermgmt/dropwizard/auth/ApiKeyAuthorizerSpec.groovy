package io.cratekube.clustermgmt.dropwizard.auth

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static org.hamcrest.Matchers.equalTo
import static spock.util.matcher.HamcrestSupport.expect

class ApiKeyAuthorizerSpec extends Specification {
  @Subject ApiKeyAuthorizer subject = new ApiKeyAuthorizer()

  @Unroll
  def 'Authorize authorizes user is in role'() {
    when:
    def result = subject.authorize(user, role)

    then:
    expect result, equalTo(output)

    where:
    user                       | role      | output
    new User(roles: ['admin']) | 'admin'   | true
    new User(roles: ['admin']) | 'badRole' | false
  }
}
