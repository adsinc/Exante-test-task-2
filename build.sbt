val circeVersion = "0.11.1"
val akkaVersion = "2.5.23"

ThisBuild / scalaVersion := "2.12.7"

// todo split on 2 modules
lazy val testTask = (project in file("."))
  .settings(
    name := "Exante test task",
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    libraryDependencies += "io.circe" %% "circe-core" % circeVersion,
    libraryDependencies += "io.circe" %% "circe-generic" % circeVersion,
    libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
  )
