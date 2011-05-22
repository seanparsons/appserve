import sbt._
import Keys._
import java.io.File

object AppserveBuild extends Build {
  val AKKA_VERSION = "1.1"
  val CAMEL_VERSION = "2.7.0"
  val SLF4J_VERSION = "1.6.1"

  //resolvers += JavaNet1Repository
  //resolvers += "Akka Repo" at "http://akka.io/repository"

  object Dependencies {
    lazy val akkaRemote = "se.scalablesolutions.akka" % "akka-remote" % AKKA_VERSION
    lazy val commonsIO = "commons-io" % "commons-io" % "2.0.1"
    lazy val camelCore = "org.apache.camel" % "camel-core" % CAMEL_VERSION
    lazy val camelJetty = "org.apache.camel" % "camel-jetty" % CAMEL_VERSION
    lazy val junit = "junit" % "junit" % "4.8.2" % "test"
    lazy val mockito = "org.mockito" % "mockito-all" % "1.8.5" % "test"
    lazy val specs2 = "org.specs2" %% "specs2" % "1.1" % "test"
    lazy val jclOverSLF4J = "org.slf4j" % "jcl-over-slf4j" % SLF4J_VERSION
    lazy val slf4j = "org.slf4j" % "slf4j-api" % SLF4J_VERSION
    lazy val logback = "ch.qos.logback"	% "logback-classic" % "0.9.28"

    //def specs2Framework = new TestFramework("org.specs2.runner.SpecsFramework")
    lazy val additionalTestDependencies = Seq(Dependencies.junit, Dependencies.mockito, Dependencies.specs2, Dependencies.jclOverSLF4J, Dependencies.slf4j)
  }
  lazy val projects = Seq(root, shared, hub, runner, example1, example2)

  lazy val root = Project("root", file(".")) aggregate(hub, runner)
  lazy val shared = Project("shared", file("shared")) settings (
    libraryDependencies += Dependencies.commonsIO
  )
  lazy val hub = Project("hub", file("hub")) dependsOn(shared) settings {
    libraryDependencies ++= Seq(Dependencies.akkaRemote, Dependencies.commonsIO)
    resolvers += "Akka Repo" at "http://akka.io/repository"
  }
  lazy val runner = Project("runner", file("runner")) dependsOn(shared) settings (
    libraryDependencies ++= Seq(Dependencies.commonsIO, Dependencies.camelCore, Dependencies.logback)
  )
  lazy val example1 = Project("example1", file("example1"), delegates = root :: Nil)
  lazy val example2 = Project("example2", file("example2"), delegates = root :: Nil)

  /*
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

  class SharedProject(info: ProjectInfo) extends AbstractAppserveProject(info) {
    lazy val commonsIO = Dependencies.commonsIO
  }

  class Example1Project(info: ProjectInfo) extends AbstractAppserveProject(info) {
    lazy val camelJetty = Dependencies.camelJetty
    lazy val camelCore = Dependencies.camelCore
  }

  class Example2Project(info: ProjectInfo) extends AbstractAppserveProject(info) {
    lazy val camelJetty = Dependencies.camelJetty
    lazy val camelCore = Dependencies.camelCore
  }
  */
}