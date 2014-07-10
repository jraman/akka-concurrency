scalaVersion := "2.11.0"

name := "Akka Concurrency"

version := "0.1-SNAPSHOT"

scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4"
)
