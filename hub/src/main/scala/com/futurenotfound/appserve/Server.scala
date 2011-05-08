package com.futurenotfound.appserve

import akka.actor.Actor._
import java.io.File
import akka.remote.RemoteServerSettings
import java.net.{InetSocketAddress, InetAddress}

object Server {
  def main(args: Array[String]): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      new Runnable() {
        def run(): Unit = {
          registry.shutdownAll()
        }
      }
    })

    val serviceName = "appserver"
    val serverPortOption = new CommandLineOption("Server Port", "Server Port", "serverPort")
    val appNameOption = new CommandLineOption("App Name", "App Name", "appName")
    val appDirOption = new CommandLineOption("Add Dir", "App Dir", "appDir")
    val options = List(serverPortOption, appNameOption, appDirOption)
    val arguments = List(new CommandLineArgument("Command", "Command"))
    def success(options: Map[String, String], arguments: Seq[String]): Unit = {
      val serverPort = options(serverPortOption.optionName).toInt
      arguments(0) match {
        case "launch" => {
          remote.start("localhost", serverPort) // Start the server
          remote.register(serviceName, actorOf(new ServerActor()))
        }
        case "push" => {
          val appName = options(appNameOption.optionName)
          val appDir = options(appDirOption.optionName)
          val serverRef = remote.actorFor(serviceName, "localhost", serverPort)
          ServerActor.addApplication(serverRef, appName, new File(appDir))
          remote.shutdownClientConnection(InetSocketAddress.createUnresolved("localhost", serverPort))
          System.exit(0)
        }
      }
    }
    CommandLineParser.run(args, options, arguments, success)
  }
}