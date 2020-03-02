package io.cratekube.example.resources

import io.cratekube.example.model.ExampleModel
import io.swagger.annotations.Api

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Api
@Path('')
@Produces('application/json')
@Consumes('application/json')
class ExampleResource {
  @GET
  List<ExampleModel> getExamples() {
    return (1..5).collect { new ExampleModel("model ${it} message") }
  }
}
