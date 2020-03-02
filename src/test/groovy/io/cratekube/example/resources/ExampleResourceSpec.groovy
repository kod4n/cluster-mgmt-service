package io.cratekube.example.resources

import spock.lang.Specification
import spock.lang.Subject

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString
import static spock.util.matcher.HamcrestSupport.expect

class ExampleResourceSpec extends Specification {
  @Subject ExampleResource resource

  def setup() {
    resource = new ExampleResource()
  }

  def 'should verify example response'() {
    when:
    def result = resource.examples

    then:
    expect result, hasSize(equalTo(5))
    expect result, everyItem(hasProperty('message', notEmptyString()))
  }
}
