organization := "com.cronos"

name := "FuturesAndThreadPools"

version := "1.0"
 
scalaVersion := Version.scala

libraryDependencies ++= Dependencies.fandtpools

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-language:_",
  "-encoding", "UTF-8"
)
