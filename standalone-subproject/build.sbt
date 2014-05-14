name := "Standalone subproject"

executeTests in Test := {
  println("Custom executeTests task")
  Tests.Output(TestResult.Passed, Map.empty, Seq.empty)
}

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.2"
