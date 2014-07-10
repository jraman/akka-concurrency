import sbt._
import sbt.Keys._

object AkkaConcurrencyBuild extends Build {

  lazy val akkaConcurrency = Project(
    id = "akka-concurrency",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "Akka Concurrency",
      organization := "com.jraman",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.11.0",
      scalacOptions ++= Seq("-feature", "-deprecation"),
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "2.2.0" % "test",
        "com.typesafe.akka" %% "akka-testkit" % "2.3.4",
        "com.typesafe.akka" %% "akka-actor" % "2.3.4"
      )

    )
  )
}
