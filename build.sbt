val circeVersion = "0.11.1"
val akkaVersion = "2.5.23"

ThisBuild / scalaVersion := "2.12.7"

lazy val commonSettings = Seq(
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

lazy val root = (project in file("."))
  .settings(
    name := "Exante test task",
  )
  .aggregate(client, server)

lazy val client = (project in file("client"))
  .settings(commonSettings)
  .settings(
    name := "Client",
  )
lazy val server = (project in file("server"))
  .settings(commonSettings)
  .settings(
    name := "Server",
    libraryDependencies += "io.circe" %% "circe-core" % circeVersion,
    libraryDependencies += "io.circe" %% "circe-generic" % circeVersion,
    libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
  )
