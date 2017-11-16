name := "text-file-sorter"

organization := "org.bruchez.olivier"

version := "1.0"

scalaVersion := "2.12.4"

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
)

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  //"-Ywarn-unused-import",
  "-Ywarn-nullary-unit",
  "-Xfatal-warnings",
  "-Xlint",
  //"-Yinline-warnings",
  "-Ywarn-dead-code",
  "-Xfuture"
)

initialCommands := "import org.bruchez.olivier.textfilesorter._"

scalafmtOnCompile in ThisBuild := true
