package com.futurenotfound.appserve

import akka.actor.Actor
import akka.actor.Actor._
import java.io.File
import sbt.Process._
import sbt.Process

case class RunnerActor(launchLocation: File) extends Actor {
  log.info("Initializing.")
  val processBuilder = Process(List("java", "-cp", "\"*\"", "com.futurenotfound.appserve.Runner"), launchLocation)
  val process = processBuilder.run()
  log.info("Running: %s".format(processBuilder))
  def receive = {
    case StopRunner => {
      process.destroy()
    }
  }

  override def postStop = process.destroy()
}

object RunnerActor {
  val timeout = 60 * 1000
}
