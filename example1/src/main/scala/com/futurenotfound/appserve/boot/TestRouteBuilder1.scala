package com.futurenotfound.appserve.boot

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy

class TestRouteBuilder1 extends RouteBuilder {
  @throws(classOf[Exception])
  def configure(): Unit = {
    from("jetty:http://0.0.0.0/cake")
      .transform(simple("${in.body}\r\n"))
      .inOut()
      .setBody(constant("<html><title>biscuits</title></html>"))
      .end()
  }
}