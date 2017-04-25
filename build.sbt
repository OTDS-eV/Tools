name := "exi-utils"

// See also
//   exi-utils script
//   exi-utils.bat
version := "2.0"

scalaVersion := "2.11.8"

organization := "de.otds.exi"

scalacOptions ++= Seq("-feature", "-deprecation", "-Xfatal-warnings")

// https://github.com/sbt/sbt-buildinfo
//   See project/buildinfo.sbt
//   => target/scala-2.11/src_managed/main/sbt-buildinfo/BuildInfo.scala
//   See also: https://stackoverflow.com/questions/8732891/can-i-access-my-scala-apps-name-and-version-as-set-in-sbt-from-code
lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, buildInfoBuildNumber),
    buildInfoPackage := "de.otds.exi.build"
  )

// OpenExi is not multi-thread safe, so parallel tests must be disabled
// http://www.scala-sbt.org/0.13/docs/Testing.html
// By default, sbt runs all tasks in parallel and within the same JVM as sbt itself.
parallelExecution in Test := false

libraryDependencies += "com.siemens.ct.exi" % "exificient" % "0.9.6"

// Command line parsing
//   https://stackoverflow.com/questions/2315912/scala-best-way-to-parse-command-line-parameters-cli
//     https://github.com/scallop/scallop
//     https://github.com/scallop/scallop/wiki
libraryDependencies += "org.rogach" % "scallop_2.11" % "2.1.1"

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.13"

// Optional dependency of commons-compress!
libraryDependencies += "org.tukaani" % "xz" % "1.6"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
