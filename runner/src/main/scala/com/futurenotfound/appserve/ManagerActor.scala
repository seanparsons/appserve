package com.futurenotfound.appserve

import akka.actor.Actor
import akka.actor.Actor._
import org.apache.camel.impl.DefaultCamelContext
import scala.collection.JavaConversions._
import org.apache.camel.builder.RouteBuilder
import org.slf4j.LoggerFactory
import java.util.jar.JarFile

case class ManagerActor[T <: RouteBuilder](val routeBuilders: Seq[Class[T]]) extends Actor {
  val camelContext = new DefaultCamelContext()
  routeBuilders.foreach{clazz =>
    log.info("Adding routes from {}.", clazz)
    camelContext.addRoutes(clazz.newInstance())
  }
  camelContext.start()
  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
    def run(): Unit = {
      registry.shutdownAll()
      camelContext.stop()
    }
  }))

  def receive = {
    case StopRunner => {
      System.exit(0)
    }
    case StartAllRoutes => camelContext.getRouteDefinitions().foreach{routeDefinition =>
      camelContext.startRoute(routeDefinition)
    }
    case StopAllRoutes => camelContext.getRouteDefinitions().foreach{routeDefinition =>
      camelContext.stopRoute(routeDefinition)
    }
  }
}

object ManagerActor {
  val logger = LoggerFactory.getLogger(this.getClass)
  val routePackage = "com.futurenotfound.appserve.boot"
  def main(args: Array[String]): Unit = {
    logger.info("Starting...")
    val port = System.getProperty("manager.port").toInt
    val routeBuilders = getPackageContent(ManagerActor.routePackage)
      .map(className => Class.forName(className))
      .collect{case clazz: Class[_] if classOf[RouteBuilder].isAssignableFrom(clazz) => clazz.asInstanceOf[Class[RouteBuilder]]}
    remote.start("localhost", port) // Start the server
    remote.register(RunnerDetails.managerServiceName, actorOf(new ManagerActor(routeBuilders)))
    logger.info("Started.")
  }

  private[this] def getPackageContent(packageName: String): List[String] = {
    val pathPrefix = packageName.replace(".", "/")
    System.getProperty("java.class.path")
      .split(";")
      .collect{case path if path.endsWith(".jar") => new JarFile(path)}
      .flatMap(jarFile => jarFile.entries().toList)
      .filter(jarEntry => jarEntry.getName().endsWith(".class") && jarEntry.getName().startsWith(pathPrefix))
      .map(jarEntry => jarEntry.getName().replace("/", ".").dropRight(6))
      .toList
  }
}