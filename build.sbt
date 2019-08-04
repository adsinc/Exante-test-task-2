val circeVersion = "0.11.1"

ThisBuild / scalaVersion := "2.12.7"

// todo split on 2 modules
lazy val testTask = (project in file("."))
  .settings(
    name := "Exante test task",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.23",
    libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.23" % Test,
    libraryDependencies += "org.scalatest" % "scalatest_2.13" % "3.0.8" % Test,
    libraryDependencies += "io.circe" %% "circe-core" % circeVersion,
    libraryDependencies += "io.circe" %% "circe-generic" % circeVersion,
    libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
  )
