package com.futurenotfound.appserve

import org.apache.camel.impl.DefaultCamelContext
import scala.collection.JavaConversions._
import org.apache.camel.builder.RouteBuilder
import org.slf4j.LoggerFactory
import java.util.jar.JarFile
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.{PatternLayout, LoggerContext}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.util.StatusPrinter
import java.util.GregorianCalendar

case class Runner[T <: RouteBuilder](val routeBuilders: Seq[Class[T]]) {
  val logger = LoggerFactory.getLogger(classOf[Runner[_]])
  def run() = {
    val camelContext = new DefaultCamelContext()
    routeBuilders.foreach{clazz =>
      logger.info("Including RouteBuilder {}", clazz)
      camelContext.addRoutes(clazz.newInstance())
    }
    camelContext.start()
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
      def run(): Unit = {
        camelContext.stop()
      }
    }))
  }
}

object Runner {
  val context = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
  context.reset()
  val patternLayout = new PatternLayout()
  patternLayout.setContext(context)
  patternLayout.setPattern("%d:%-5level %logger - %msg%n")
  patternLayout.start()
  val appender = new FileAppender[ILoggingEvent]()
  appender.setFile("runner.log")
  appender.setContext(context)
  appender.setLayout(patternLayout)
  appender.start()
  val rootLogger = context.getLogger("root")
  rootLogger.addAppender(appender);
  context.start()
  StatusPrinter.printInCaseOfErrorsOrWarnings(context);

  val logger = LoggerFactory.getLogger(this.getClass)
  val routePackage = "com.futurenotfound.appserve.boot"
  def main(args: Array[String]): Unit = {
    logger.info("Starting..." + (new GregorianCalendar()).toString())
    val routeBuilders = getPackageContent(Runner.routePackage)
      .map(className => Class.forName(className))
      .collect{case clazz: Class[_] if classOf[RouteBuilder].isAssignableFrom(clazz) => clazz.asInstanceOf[Class[RouteBuilder]]}
    new Runner(routeBuilders).run()
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