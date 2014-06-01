name := "Standalone subproject"

scalaVersion := "2.11.0"

libraryDependencies ++= Seq(
  "2m" %% "standalone-subproject-dependency" % "1.0.1",
  "org.scalatest" % "scalatest_2.11" % "2.1.6" % "test"
)
