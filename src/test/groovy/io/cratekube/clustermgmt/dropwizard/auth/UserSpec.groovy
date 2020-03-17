package io.cratekube.clustermgmt.dropwizard.auth

import spock.lang.Specification
import spock.lang.Subject

import javax.ws.rs.ForbiddenException

class UserSpec extends Specification {
  @Subject User subject
  List<String> roles

  def setup() {
    roles = ['admin']
    subject = new User(roles: roles)
  }

  def 'CheckUserInRole should throw ForbiddenException'() {
    when:
    subject.checkUserInRole('bad-role')

    then:
    thrown ForbiddenException
  }
}
