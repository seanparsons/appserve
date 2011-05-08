package com.futurenotfound.appserve

import java.io.File
import org.apache.commons.io.FileUtils
import akka.actor._
import akka.actor.Actor._

case class UpdateApplication(name: String, files: Map[String, Array[Byte]])

case class RemoveApplication(name: String)

case object ReportApplications

case object RestartServer

case class ServerActor() extends Actor {
  val baseLocation = new File(".")
  val runnerLocation = new File(baseLocation, "runner").ensuring(file => file.exists(), "Runner location does not exist.")
  val dropLocation = new File(baseLocation, "drop")
  dropLocation.mkdir()
  val launchLocation = new File(baseLocation, "launch")
  launchLocation.mkdir()
  
  private[this] def createRunner = actorOf(new RunnerActor(launchLocation)).start
  private[this] def applicationLocation(name: String) = new File(dropLocation, name)
  private[this] var runnerActor: ActorRef = null

  def receive = {
    case RestartServer => {
      if (runnerActor != null) {
        log.info("Restarting runner.")
        runnerActor !! StopRunner
        log.info("StopRunner message sent.")
        runnerActor.stop()
        log.info("Stopped actor.")
      }
      launchLocation.listFiles().foreach(file => file.delete())
      // Combine the files for each of the applications with those of the runner and put them into the launch location.
      (dropLocation.listFiles().flatMap(file => file.listFiles()) ++ runnerLocation.listFiles())
        .foreach(file => FileUtils.copyFileToDirectory(file, launchLocation))
      log.info("Files to launch recombined: %s", launchLocation.listFiles())
      runnerActor = createRunner
      log.info("Runner recreated.")
    }
    case UpdateApplication(name, files) => {
      log.info("Updating application %s".format(name))
      val appLocation = applicationLocation(name)
      appLocation.delete()
      appLocation.mkdir()
      files.foreach{case (filename, fileContents) =>
        val file = new File(appLocation, filename)
        FileUtils.writeByteArrayToFile(file, fileContents)
      }
      log.info("Updated application %s".format(name))
    }
    case RemoveApplication(name) => applicationLocation(name).delete()
    case ReportApplications => {
      self.reply_?(dropLocation.listFiles().map(applicationDirectory => (applicationDirectory.getName(), applicationDirectory.listFiles().map(applicationFile => applicationFile.getName()))))
    }
  }
}

object ServerActor {
  def addApplication(serverActor: ActorRef, name: String, dir: File) = {
    serverActor !! new UpdateApplication(name, dir.listFiles().collect{case file if file.isFile() => (file.getName, FileUtils.readFileToByteArray(file))}.toMap)
    serverActor !! RestartServer
  }
  def startInstance = actorOf(new ServerActor()).start()
}