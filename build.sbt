ThisBuild / scalaVersion := "2.12.7"

// todo split on 2 modules
lazy val testTask = (project in file("."))
  .settings(
    name := "Exante test task",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.23",
    libraryDependencies += "org.scalatest" % "scalatest_2.13" % "3.0.8" % "test"
  )
