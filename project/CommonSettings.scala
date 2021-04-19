import sbt.Keys._
import sbt._
import sbtstudent.AdditionalSettings

object CommonSettings {
  lazy val commonSettings = Seq(
    organization := "com.lunatech.pi",
    version := "1.0.0",
    Compile / scalacOptions ++= CompileOptions.compileOptions,
    //javacOptions in Compile ++= Seq("--release", "11"),
    Compile / unmanagedSourceDirectories := List((Compile / scalaSource).value, (Compile / javaSource).value),
    Test / unmanagedSourceDirectories := List((Test / scalaSource ).value, (Test / javaSource).value),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
    Test / logBuffered := false,
    Test / parallelExecution := false,
    GlobalScope / parallelExecution := false,
    Test / fork := false,
    packageSrc / publishArtifact := false,
    packageDoc / publishArtifact := false,
    libraryDependencies ++= Dependencies.dependencies,
    libraryDependencies ++= Dependencies.crossDependencies.map(_.cross(CrossVersion.for3Use2_13))
  ) ++
    AdditionalSettings.initialCmdsConsole ++
    AdditionalSettings.initialCmdsTestConsole ++
    AdditionalSettings.cmdAliases

  lazy val configure: Project => Project = (proj: Project) => {
    proj
  }
}
