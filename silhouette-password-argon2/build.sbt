import Dependencies._

libraryDependencies ++= Seq(
  Library.argon2,
  Library.Specs2.core % Test
)

enablePlugins(Doc)

val repo: String = "https://s01.oss.sonatype.org"
publishTo := {
  if(version.value.endsWith("-SNAPSHOT")) {
    Some("Sonatype Nexus Repository Manager" at s"$repo/content/repositories/snapshots/")
  }
  else {
    Some("Sonatype Nexus Repository Manager" at s"$repo/service/local/staging/deploy/maven2/")
  }
}
