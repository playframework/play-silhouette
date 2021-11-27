import Dependencies._

libraryDependencies ++= Seq(
  Library.Play.cache,
  Library.Play.ws,
  Library.Play.openid,
  Library.Play.jsonJoda,
  Library.jwtCore,
  Library.jwtApi,
  Library.apacheCommonLang,
  Library.Play.specs2 % Test,
  Library.Specs2.matcherExtra % Test,
  Library.Specs2.mock % Test,
  Library.scalaGuice % Test,
  Library.akkaTestkit % Test
)

enablePlugins(PlayScala, Doc)

unmanagedSourceDirectories in Compile += {
  baseDirectory.value / (if(Util.priorTo213(scalaVersion.value)) "app-2.13-" else "app-2.13+")
}

val repo: String = "https://s01.oss.sonatype.org"
publishTo := {
  if(version.value.endsWith("-SNAPSHOT")) {
    Some("Sonatype Nexus Repository Manager" at s"$repo/content/repositories/snapshots/")
  }
  else {
    Some("Sonatype Nexus Repository Manager" at s"$repo/service/local/staging/deploy/maven2/")
  }
}
