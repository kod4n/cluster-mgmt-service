package io.cratekube.clustermgmt

import spock.lang.Specification
import spock.lang.Subject

class AppSpec extends Specification {
  @Subject App app

  def setup() {
    app = new App()
  }

  def 'should verify no exception is thrown on main'() {
    when:
    App.main()

    then:
    noExceptionThrown()
  }

  def 'should verify no exception is thrown on run'() {
    when:
    app.run(null, null)

    then:
    noExceptionThrown()
  }
}
