package com.futurenotfound.appserve.boot

import org.apache.camel.builder.RouteBuilder

class TestRouteBuilder2 extends RouteBuilder {
  @throws(classOf[Exception])
  def configure(): Unit = {
    from("jetty:http://0.0.0.0/biscuits")
      .inOut()
      .setBody(constant("<html><title>cake</title></html>"))
      .end()
  }
}