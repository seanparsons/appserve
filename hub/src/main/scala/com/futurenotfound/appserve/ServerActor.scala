package com.futurenotfound.appserve

import akka.actor._
import akka.actor.Actor._
import org.apache.commons.io.{IOUtils, FileUtils}
import java.io.{FilenameFilter, FileOutputStream, IOException, File}
import scala.collection.JavaConversions._
import java.util.jar.{JarEntry, JarFile}
import org.slf4j.LoggerFactory

case class UpdateApplication(name: String, files: Set[FileData])

case class SendTheseUpdates(name: String, files: Set[FileData])

case class AlreadyHaveDifferingVersion(name: String, applicationName: String, currentChecksum: String, sentChecksum: String)

case class FileChunk(name: String, data: Array[Byte])

case object FinishedSendingApplication

case class RemoveApplication(name: String)

case object ReportApplications

case object RestartServer

case object SuccessfulUpdate

case class UnsuccessfulUpdate(conflictDetail: AlreadyHaveDifferingVersion)

case class BundleJarFile(name: String, contents: Set[FileData]) {
  def this(jarFile: JarFile) = this(jarFile.getName, IO.classFilesFromJar(jarFile).map(entry => FileData(jarFile, entry)).toSet)
  def this(file: File) = this(new JarFile(file))
}

case class ApplicationDetails(name: String, files: Seq[BundleJarFile]) {
  def this(name: String, file: File) = this(name, IO.filesFromDir(file).map(file => new BundleJarFile(file)))
  def this(dir: File) = this(dir.ensuring(dir.isDirectory, "File instance passed is not a directory").getName, dir)
  lazy val classFilesMap = files.flatMap(bundleFile => bundleFile.contents).map(fileData => (fileData.name, fileData.checksum)).toMap
  def conflictWith(otherApplication: ApplicationDetails) = {
    classFilesMap.find(pair => otherApplication.classFilesMap.get(pair._1).map(_ != pair._2).getOrElse(false))
      .collect{case (filename, checksum) => AlreadyHaveDifferingVersion(filename, name, checksum, otherApplication.classFilesMap(filename))}
  }
}

case class ServerActor() extends Actor {
  val baseLocation = new File(".")
  val runnerLocation = new File(baseLocation, "runner").ensuring(file => file.exists(), "Runner location does not exist.")
  val dropLocation = new File(baseLocation, "drop")
  dropLocation.mkdir()
  val launchLocation = new File(baseLocation, "launch")
  launchLocation.mkdir()
  val tempLocation = new File(baseLocation, "temp")
  tempLocation.mkdir()
  
  private[this] def deployAndStartRunner = {
    launchLocation.listFiles().foreach(file => file.delete())
    // Combine the files for each of the applications with those of the runner and put them into the launch location.
    (dropLocation.listFiles().flatMap(file => file.listFiles()) ++ runnerLocation.listFiles())
      .foreach(file => FileUtils.copyFileToDirectory(file, launchLocation))
    actorOf(new RunnerActor(launchLocation)).start
  }
  private[this] def applicationLocation(name: String) = new File(dropLocation, name)
  private[this] var runnerActor = deployAndStartRunner

  def writeChunk(file: File, chunk: Array[Byte]) = {
    val outputStream = new FileOutputStream(file, true)
    try {
      IOUtils.write(chunk, outputStream)
    } finally {
      IOUtils.closeQuietly(outputStream)
    }
  }

  private[this] def conflicts(applicationName: String): Option[AlreadyHaveDifferingVersion] = {
    // Collate applications, with their files, except the one being replaced.
    val applications = dropLocation.listFiles()
                                   .map(file => new ApplicationDetails(file))
                                   .filterNot(application => application.name != applicationName)
    val newApplication = new ApplicationDetails(tempLocation)
    applications.foldLeft(None: Option[AlreadyHaveDifferingVersion]){(possibleResult, applicationDetails) =>
      possibleResult match {
        case some: Some[AlreadyHaveDifferingVersion] => some
        case None => applicationDetails.conflictWith(newApplication)
      }
    }
  }

  def filesNotStoredLocally(contents: Set[FileData]) = contents -- IO.filesFromDir(launchLocation).map(file => FileData(file)).toSet

  def receive = {
    case RestartServer => {
      if (runnerActor != null) {
        log.info("Restarting runner.")
        runnerActor !! StopRunner
        log.info("StopRunner message sent.")
        runnerActor.stop()
        log.info("Stopped actor.")
      }
      runnerActor = deployAndStartRunner
      log.info("Runner recreated.")
      self.reply(true)
    }
    case UpdateApplication(applicationName, files) => {
      log.info("Updating application %s".format(applicationName))
      tempLocation.delete()
      tempLocation.mkdir()
      // Compare file checksums from detail sent and reply with file details for those files that need sending.
      self.reply(SendTheseUpdates(applicationName, filesNotStoredLocally(files)))
      // Become an actor that only accepts FileChunk entries and FinishedSendingApplication entries.
      // TODO: Make it handle those only from the sender that sent the UpdateApplication message.
      become {
        case FileChunk(filename, data) => self.reply(writeChunk(new File(tempLocation, filename), data))
        case FinishedSendingApplication => {
          // Check for conflicts.
          conflicts(applicationName) match {
            case Some(conflictDetail) => self.reply(UnsuccessfulUpdate(conflictDetail))
            case None => {
              val appLocation = applicationLocation(applicationName)
              appLocation.delete()
              FileUtils.copyDirectory(tempLocation, appLocation)
              self.reply(SuccessfulUpdate)
            }
          }
          // If there are conflicts, then report back to the sender.
          // If there are none, replace the old application with this one.
          unbecome
          log.info("Updated application %s".format(applicationName))
        }
      }
    }
    case RemoveApplication(name) => self.reply(applicationLocation(name).delete())
    case ReportApplications => {
      self.reply(dropLocation.listFiles().map(applicationDirectory => (applicationDirectory.getName(), applicationDirectory.listFiles().map(applicationFile => applicationFile.getName()))))
    }
  }
}

object ServerActor {
  val logger = LoggerFactory.getLogger("ServerActor")
  val fileBatchSize = 1024 * 1024
  def addApplication(serverActor: ActorRef, name: String, dir: File) = {
    val appFiles = IO.filesFromDir(dir)
    logger.info("{}", appFiles)
    val sendTheseUpdates = (serverActor !! new UpdateApplication(name, appFiles.map(file => FileData(file)).toSet)).get.asInstanceOf[SendTheseUpdates]
    logger.info("{}", sendTheseUpdates)
    appFiles.filter(file => sendTheseUpdates.files.exists(fileData => fileData.name == file.getName()))
            .flatMap(file => FileUtils.readFileToByteArray(file).grouped(fileBatchSize).map(bytes => (file.getName, bytes)).toList)
            .foreach{fileAndBytes =>
              logger.info("Sending chunk of {} bytes for file {}.", fileAndBytes._2.size, fileAndBytes._1)
              serverActor !! FileChunk(fileAndBytes._1, fileAndBytes._2)
            }
    serverActor !! FinishedSendingApplication
    serverActor !! RestartServer
  }
  def startInstance = actorOf(new ServerActor()).start()
}