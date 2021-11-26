import Dependencies._

libraryDependencies ++= Seq(
  Library.googleAuth,
  Library.Play.specs2 % Test
)

enablePlugins(Doc)

publishTo := Some("Sonatype Nexus Repository Manager" at "https://s01.oss.sonatype.org/content/repositories/snapshots/")
