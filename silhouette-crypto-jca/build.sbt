import Dependencies._

libraryDependencies ++= Seq(
  Library.Specs2.core % Test,
  Library.Specs2.matcherExtra % Test
)

enablePlugins(Doc)

publishTo := Some("Sonatype Nexus Repository Manager" at "https://s01.oss.sonatype.org/content/repositories/snapshots/")
