package com.futurenotfound.appserve.boot

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition

class TestRouteBuilder2 extends RouteBuilder {
  @throws(classOf[Exception])
  def configure(): Unit = {
    from("jetty:http://0.0.0.0/biscuits")
      .inOut()
      .setBody(constant("<html><title>cake</title></html>"))
      .end()
  }
}