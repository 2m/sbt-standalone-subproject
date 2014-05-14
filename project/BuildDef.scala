import sbt._
import sbt.Keys._

object BuildDef extends Build {

  val root = Project(
    id = "sbt-standalone-subproject",
    base = file("."),
    settings = Defaults.defaultSettings ++ Seq(
      commands += firstCommand
    )
  ) dependsOn(akkaActor, standaloneSubproject)

  lazy val akkaActor = ProjectRef(file("akka-actor").toURI, "akka-actor")

  lazy val standaloneSubproject = ProjectRef(file("standalone-subproject").toURI, "standalone-subproject")

  def firstCommand = Command.command("cloneSubroject") { st =>

    val extracted = Project.extract(st)

    val subResolved = Project.getProject(standaloneSubproject, extracted.structure).get

    lazy val subProject = Project(
      id = subResolved.id + "-head",
      base = subResolved.base,
      settings = subResolved.settings ++ Seq(
        allDependencies := allDependencies.value.filterNot { moduleId =>
          moduleId.organization == "com.typesafe.akka"
        }
      ),
      configurations = subResolved.configurations
    )

    //val appendSettings = Load.transformSettings(Load.projectScope(<reference of subProject>), extracted.currentRef.build, extracted.rootProject, settings)
    //val newStructure = Load.reapply(extracted.session.original ++ appendSettings, extracted.structure)
    //val state = Project.setProject(extracted.session, newStructure, st)

    val (state2, _) = extracted.runTask(test in (standaloneSubproject, Test), st)

    // throws java.lang.RuntimeException: standalone-subproject-head/test:test is undefined
    val (state3, _) = extracted.runTask(test in (subProject, Test), state2)

    state3
  }

}
