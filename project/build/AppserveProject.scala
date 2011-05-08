import sbt._
import sbt.CompileOrder._
import java.io.File
import reaktor.scct.ScctProject

class AppserveProject(info: ProjectInfo) extends DefaultProject(info) with IdeaProject {
  lazy val shared = project("shared", "shared", new SharedProject(_))
  lazy val hub = project("hub", "hub", new HubProject(_), shared)
  lazy val runner = project("runner", "runner", new RunnerProject(_), shared)
  lazy val example1: ExampleProject = project("example1", "example1", new ExampleProject(_))
  lazy val example2: ExampleProject = project("example2", "example2", new ExampleProject(_))
  val AKKA_VERSION = "1.0"
  val CAMEL_VERSION = "2.6.0"
  val SLF4J_VERSION = "1.6.1"
  val scalaToolsSnapshots = "Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots/"
  
  object Dependencies {
    lazy val akkaRemote = "se.scalablesolutions.akka" % "akka-remote" % AKKA_VERSION
    lazy val commonsIO = "commons-io" % "commons-io" % "2.0.1"
    lazy val camelCore = "org.apache.camel" % "camel-core" % CAMEL_VERSION
    lazy val camelJetty = "org.apache.camel" % "camel-jetty" % CAMEL_VERSION
    lazy val junit = "junit" % "junit" % "4.8.2" % "test"
    lazy val mockito = "org.mockito" % "mockito-all" % "1.8.5" % "test"
    lazy val specs2 = "org.specs2" %% "specs2" % "1.1" % "test"
    lazy val jclOverSLF4J = "org.slf4j" % "jcl-over-slf4j" % SLF4J_VERSION % "test"
    lazy val slf4j = "org.slf4j" % "slf4j-api" % SLF4J_VERSION
    lazy val logback = "ch.qos.logback"	% "logback-classic" % "0.9.28"
  }

  val distPath = Path.fromString(info.projectPath, "./dist")
  def copyProject(project: AbstractAppserveProject, subPath: String, additionalFiles: PathFinder): Any = {
    val projectName = project.projectName.get.get
    val projectExternalDependencies = (project.runClasspath ** new SimpleFileFilter(file => file.getName().endsWith(".jar")))
    val projectInternalDependencies = project.dependencies.foldLeft(Path.emptyPathFinder)((left, right: Project) => left +++ right.asInstanceOf[AbstractAppserveProject].jarPath)
    FileUtilities.copyFlat(
      (projectInternalDependencies +++ projectExternalDependencies +++ project.jarPath +++ Path.fromFile(project.buildScalaInstance.libraryJar) +++ additionalFiles).get,
      Path.fromString(distPath, subPath),
      log)
    log.info("Copied project " + projectName)
  }
  def copyProject(project: AbstractAppserveProject, additionalFiles: PathFinder): Any = copyProject(project, project.projectName.get.get, additionalFiles)
  def copyProject(project: AbstractAppserveProject, subPath: String): Any = copyProject(project, subPath, Path.emptyPathFinder)
  def copyProject(project: AbstractAppserveProject): Any = copyProject(project, project.projectName.get.get, Path.emptyPathFinder)
  lazy val dist = task {
    FileUtilities.clean(distPath, log)
    copyProject(example1)
    copyProject(example2)
    copyProject(hub, (hub.info.projectPath / "scripts") * AllPassFilter)
    copyProject(runner, "hub/runner")
    None
  }.dependsOn(this.`package`)

  abstract class AbstractAppserveProject(info: ProjectInfo) extends DefaultProject(info) with IdeaProject with ScctProject {
    def specs2Framework = new TestFramework("org.specs2.runner.SpecsFramework")
    override def testFrameworks = super.testFrameworks ++ Seq(specs2Framework)
    lazy val junit = Dependencies.junit
    lazy val mockito = Dependencies.mockito
    lazy val specs2 = Dependencies.specs2
    lazy val jclOverSLF4J = Dependencies.jclOverSLF4J
    lazy val slf4j = Dependencies.slf4j
  }

  class RunnerProject(info: ProjectInfo) extends AbstractAppserveProject(info) {
    lazy val commonsIO = Dependencies.commonsIO
    lazy val camelCore = Dependencies.camelCore
    lazy val logback = Dependencies.logback
  }

  class HubProject(info: ProjectInfo) extends AbstractAppserveProject(info) with AkkaProject {
    lazy val akkaRemote = Dependencies.akkaRemote
    lazy val commonsIO = Dependencies.commonsIO
    override def mainClass = Some("com.futurenotfound.appserve.ServerActor")
  }

  class SharedProject(info: ProjectInfo) extends AbstractAppserveProject(info)

  class ExampleProject(info: ProjectInfo) extends AbstractAppserveProject(info) {
    lazy val camelJetty = Dependencies.camelJetty
    lazy val camelCore = Dependencies.camelCore
  }
}