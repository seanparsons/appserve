package com.futurenotfound.appserve

import akka.actor.Actor._
import java.io.File
import org.apache.commons.io.FileUtils
import akka.actor._

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
  private[this] var runnerActor = createRunner

  def receive = {
    case RestartServer => {
      runnerActor !! StopRunner
      runnerActor.stop()
      runnerLocation.listFiles().foreach(file => file.delete())
      // Combine the files for each of the applications with those of the runner and put them into the launch location.
      (dropLocation.listFiles().flatMap(file => file.listFiles()) ++ runnerLocation.listFiles())
        .foreach(file => FileUtils.copyFileToDirectory(file, launchLocation))
      runnerActor = createRunner
    }
    case UpdateApplication(name, files) => {
      val appLocation = applicationLocation(name)
      appLocation.delete()
      appLocation.mkdir()
      files.foreach{case (filename, fileContents) =>
        val file = new File(appLocation, filename)
        FileUtils.writeByteArrayToFile(file, fileContents)
      }
    }
    case RemoveApplication(name) => applicationLocation(name).delete()
    case ReportApplications => {
      self.reply_?(dropLocation.listFiles().map(applicationDirectory => (applicationDirectory.getName(), applicationDirectory.listFiles().map(applicationFile => applicationFile.getName()))))
    }
  }
}

object ServerActor {
  def addApplication(serverActor: ActorRef, name: String, dir: File) = {
    serverActor ! new UpdateApplication(name, dir.listFiles().collect{case file if file.isFile() => (file.getName, FileUtils.readFileToByteArray(file))}.toMap)
    serverActor ! RestartServer
  }
  def startInstance = actorOf(new ServerActor()).start()

  def main(args: Array[String]): Unit = {

  }
}