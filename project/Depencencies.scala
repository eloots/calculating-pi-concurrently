import sbt._

object Version {
  val scala        = "2.13.1"
  val scalaTest    = "3.0.8"
}

object Library {
  val scalaTest    = "org.scalatest"          %% "scalatest"                % Version.scalaTest
}

object Dependencies {
  import Library._

  val fandtpools = List(
    scalaTest % "test"
  )
}
