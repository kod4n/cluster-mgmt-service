package io.cratekube.example.resources

import io.cratekube.example.BaseIntegrationSpec
import io.cratekube.example.model.ExampleModel

import javax.ws.rs.core.GenericType

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString
import static spock.util.matcher.HamcrestSupport.expect

class ExampleResourceIntegrationSpec extends BaseIntegrationSpec {
  def 'should get list response when executing GET'() {
    when:
    def result = baseRequest().get(new GenericType<List<ExampleModel>>() {})

    then:
    expect result, hasSize(equalTo(5))
    expect result, everyItem(hasProperty('message', notEmptyString()))
  }
}
