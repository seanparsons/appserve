package com.futurenotfound.appserve

import org.apache.camel.builder.RouteBuilder
import akka.actor.Actor._
import akka.actor._
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.component.direct.DirectEndpoint
import org.specs2.mutable._
import org.specs2.mock.Mockito
import akka.actor.ActorRef
import org.apache.camel.{CamelContext, ServiceStatus}
import org.specs2.specification._
import org.specs2.execute.{Result, Success}
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator

class TestRouteBuilder extends RouteBuilder {
  @throws(classOf[Exception])
  def configure(): Unit = {
    TestRouteBuilder.instance = this
    from("direct:test")
      .id("testroute")
      .to("mock:test").end()
  }
}

object TestRouteBuilder {
  var instance: TestRouteBuilder = null
}

class ManagerActorSpec extends Specification with Mockito {
  val loggerContext = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
  val configurator = new JoranConfigurator()
  configurator.setContext(loggerContext)
  loggerContext.reset()

  override def is =
  "Runner should"                                     ^ sequential ^
    "shutdown the camel route correctly"                    ! AroundManagerActor(stopRunner(_)) ^
    "stop all the routes when told to"                      ! AroundManagerActor(stopRoutes(_)) ^
    "restart all the routes after stopping them"            ! AroundManagerActor(stopAndStartRoutes(_)) ^
    "restart multiple times"                                ! AroundManagerActor(stopAndStartRoutesTwice(_)) ^
                                                            end

  object AroundManagerActor extends AroundOutside[ActorRef] {
    def around[T <% Result](example: => T) = {
      val result = example
      outside.stop()
      result
    }

    def outside = actorOf(new Runner(List(classOf[TestRouteBuilder]))).start()
  }

  def stopRunner(managerActor: ActorRef) = {
    managerActor ! StopRunner
    TestRouteBuilder.instance.getContext().getStatus() must eventually(be(ServiceStatus.Stopped))
  }
  def stopRoutes(managerActor: ActorRef) = {
    managerActor ! StopAllRoutes
    TestRouteBuilder.instance.getContext().getRouteStatus("testroute") must eventually(be(ServiceStatus.Stopped))
  }
  def startRoutes(managerActor: ActorRef) = {
    managerActor ! StartAllRoutes
    success
  }
  def routesStarted(managerActor: ActorRef) = {
    TestRouteBuilder.instance.getContext().getRouteStatus("testroute") must eventually(be(ServiceStatus.Started))
  }
  def routesRestarted(managerActor: ActorRef) = startRoutes(managerActor) and routesStarted(managerActor)
  def stopAndStartRoutes(managerActor: ActorRef) = stopRoutes(managerActor) and routesRestarted(managerActor)
  def stopAndStartRoutesTwice(managerActor: ActorRef) = stopAndStartRoutes(managerActor) and stopAndStartRoutes(managerActor)
}