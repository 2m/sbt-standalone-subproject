import experimental.DependsOnLocal._


lazy val root = (
    Project("sbt-standalone-subproject-root", base = file(".")).
    settings(
      // This was here but makes no sense, as this project is just an aggregated, AFAICT.
      scalaVersion := "2.11.0"
    ).
    aggregate(subproject)
    // TODO - Once the PGP plguin supports autoplugins, this will work to disable all
    // ivy/publish things from root, so only subprojects are published/wired.
    //  .disablePlugins(plugins.IvyPlugin)
)


lazy val subProjectDep = ProjectRef(file("standalone-subproject-dependency").toURI, "standalone-subproject-dependency")

lazy val subproject = (
  Project("standalone-subproject", file("standalone-subproject")).
  dependsOnLocal(subProjectDep, "2m" %% "standalone-subproject-dependency" % "1.0.1")
)

commands ++= Seq(
  Command.command("breakLocalDep") { state: State =>
     setLocalDisabled(subProjectDep, true)
     "reload" :: state
  },
  Command.command("restoreLocalDep") { state: State =>
     setLocalDisabled(subProjectDep, false)
     "reload" :: state
  }
)
