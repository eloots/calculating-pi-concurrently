/***************************************************************
  *      THIS IS A GENERATED FILE - EDIT AT YOUR OWN RISK      *
  **************************************************************
  *
  * Use the mainadm command to generate a new version of
  * this build file.
  *
  * See https://github.com/lightbend/course-management-tools
  * for more details
  *
  */

import sbt._

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val `calculating-pi-main` = (project in file("."))
  .aggregate(
    common,
    `step_001_calculating_pi_with_futures`,
    `step_002_calculating_pi_with_akka_streams_base`,
    `step_003_calculating_pi_with_akka_streams_mapAsync`,
    `step_004_calculating_pi_with_akka_streams_mapAsync_logging`,
    `step_005_calculating_pi_with_akka_streams_substreams`,
    `step_006_calculating_pi_with_akka_streams_graph`,
    `step_007_calculating_pi_with_akka_streams_linear`
  )
  .settings(ThisBuild / scalaVersion := Version.scalaVersion)
  .settings(CommonSettings.commonSettings: _*)

lazy val common = project
  .settings(CommonSettings.commonSettings: _*)

lazy val `step_001_calculating_pi_with_futures` = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")

lazy val `step_002_calculating_pi_with_akka_streams_base` = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")

lazy val `step_003_calculating_pi_with_akka_streams_mapAsync` = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")

lazy val `step_004_calculating_pi_with_akka_streams_mapAsync_logging` = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")

lazy val `step_005_calculating_pi_with_akka_streams_substreams` = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")

lazy val `step_006_calculating_pi_with_akka_streams_graph` = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")

lazy val `step_007_calculating_pi_with_akka_streams_linear` = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")
       