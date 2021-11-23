import Dependencies._

libraryDependencies ++= Seq(
  Library.argon2,
  Library.Specs2.core % Test
)

enablePlugins(Doc)
