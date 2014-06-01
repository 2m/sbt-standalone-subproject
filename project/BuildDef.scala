import sbt._
import sbt.BuildPaths._
import sbt.BuildStreams._
import sbt.compiler.Eval
import sbt.inc.{Locate, FileValueCache}
import sbt.Keys._
import sbt.Select
import sbt.Select
import sbt.Select
import scala.collection.mutable

object BuildDef extends Build {

  val root = Project(
    id = "sbt-standalone-subproject",
    base = file("."),
    settings = Defaults.defaultSettings ++ Seq(
      commands ++= Seq(testWithLibraryDep, testWithProjectDep),
      scalaVersion := "2.11.0"
    )
  )

  def testWithLibraryDep = Command.command("testWithLibraryDep") { s =>
    loadAndTest(s, Load.defaultLoad)
    s
  }

  def testWithProjectDep = Command.command("testWithProjectDep") { s =>
    loadAndTest(s, CustomLoader.defaultLoad)
    s
  }

  def loadAndTest(s: State, loader: (State, File, Logger, Boolean, List[URI]) => (() => Eval, sbt.BuildStructure)) {
    val base = file("standalone-subproject")
    val projectRef = ProjectRef(base.toURI, "standalone-subproject")

    val (eval, structure) = loader(s, base, s.log, false, Nil)

    val session = Load.initialSession(structure, eval, s)
    val state = Project.setProject(session, structure, s)

    Project.extract(state).runTask(test in (projectRef, Test), state)
  }

}
