import sbt._

object Version {
  val scala        = "2.11.0"
  val scalaParsers = "1.0.1"
  val scalaTest    = "2.1.4"
}

object Library {
  val scalaParsers = "org.scala-lang.modules" %% "scala-parser-combinators" % Version.scalaParsers
  val scalaTest    = "org.scalatest"          %% "scalatest"                % Version.scalaTest
}

object Dependencies {

  import Library._

  val fandtpools = List(
    scalaParsers,
    scalaTest % "test"
  )
}
