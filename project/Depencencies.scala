import sbt._

object Version {
  val scala        = "2.13.1"
  val scalaTest    = "3.0.8"
  val akka         = "2.6.3"
}

object Library {
  val akkaStreams  = "com.typesafe.akka"      %% "akka-stream"              % Version.akka
  val scalaTest    = "org.scalatest"          %% "scalatest"                % Version.scalaTest
}

object Dependencies {
  import Library._

  val fandtpools = List(
    akkaStreams,
    scalaTest % "test"
  )
}
