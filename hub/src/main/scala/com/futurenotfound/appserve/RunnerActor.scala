package com.futurenotfound.appserve

import akka.actor.Actor
import akka.actor.Actor._
import java.io.File
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

case class RunnerActor(launchLocation: File, managerPort: Int) extends Actor {
  val logger = LoggerFactory.getLogger(this.getClass)
  val processBuilder = new ProcessBuilder("java", "-Dmanager.port=%s".format(managerPort), "-cp", "\"*\"", "com.futurenotfound.appserve.ManagerActor")
    .directory(launchLocation)
  val process = processBuilder.start()
  val keepRunning = new AtomicBoolean(true)
  val outputThread = new Thread(new Runnable(){
    def run(): Unit = {
      while (keepRunning.get()) {
        val stdOut = Stream.continually(process.getInputStream().read()).takeWhile(_ >= 0).mkString
        if (stdOut != null && stdOut.length() > 0) log.info(stdOut)
        val stdErr = Stream.continually(process.getErrorStream().read()).takeWhile(_ >= 0).mkString
        if (stdErr != null && stdErr.length() > 0) log.info(stdErr)
        Thread.sleep(100)
      }
    }
  })
  outputThread.start()
  logger.info("{}", processBuilder.command())
  val managerActor = remote.actorFor(RunnerDetails.managerServiceName, RunnerActor.timeout, "localhost", managerPort).start()
  def receive = {
    case StopRunner => {
      keepRunning.set(false)
      Thread.sleep(200)
      self.reply_?(managerActor !! StopRunner)
      managerActor.stop()
    }
  }
}

object RunnerActor {
  val timeout = 60 * 1000
}
