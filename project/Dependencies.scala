import sbt.Keys.libraryDependencies
import sbt._

object Version {
  val akkaVer           = "2.6.14"
  val logbackVer        = "1.2.3"
  val scalaVersion      = "3.0.0-RC2"
}

object Dependencies {
  
  private val akkaDeps = Seq(
    "com.typesafe.akka"             %% "akka-actor-typed",
    "com.typesafe.akka"             %% "akka-serialization-jackson",
    "com.typesafe.akka"             %% "akka-cluster-typed",
    "com.typesafe.akka"             %% "akka-cluster-sharding-typed",
    "com.typesafe.akka"             %% "akka-persistence-typed",
    "com.typesafe.akka"             %% "akka-slf4j",
    "com.typesafe.akka"             %% "akka-stream",
    "com.typesafe.akka"             %% "akka-discovery",
    "com.typesafe.akka"             %% "akka-serialization-jackson",
  ).map (_ % Version.akkaVer)

  private val logbackDeps = Seq (
    "ch.qos.logback"                 %  "logback-classic",
  ).map (_ % Version.logbackVer)

  val dependencies: Seq[ModuleID] =
    logbackDeps

    val crossDependencies: Seq[ModuleID] =
    akkaDeps
}
