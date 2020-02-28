organization := "com.cronos"

name := "FuturesAndThreadPools"

version := "1.0"
 
scalaVersion := Version.scala

libraryDependencies ++= Dependencies.fandtpools

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8"
)
