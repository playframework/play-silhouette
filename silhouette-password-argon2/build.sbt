import Dependencies._

libraryDependencies ++= Seq(
  Library.argon2,
  Library.Specs2.core % Test
)

enablePlugins(Doc)

publishTo := Some("Sonatype Nexus Repository Manager" at "https://s01.oss.sonatype.org/content/repositories/snapshots/")
