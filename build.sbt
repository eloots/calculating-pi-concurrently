organization := "com.cronos"

name := "FuturesAndThreadPools"

version := "1.0"
 
scalaVersion := Version.scala

libraryDependencies ++= Dependencies.fandtpools

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)
