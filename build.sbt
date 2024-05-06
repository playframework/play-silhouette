import Dependencies.Library

lazy val scala213: String = "2.13.14"
lazy val scala3: String = "3.3.3"
lazy val supportedScalaVersions: Seq[String] = Seq(scala213, scala3)

Global / evictionErrorLevel   := Level.Info

val sonatypeProfile = "org.playframework"

val previousVersion: Option[String] = None // Some("0.8.0")

val commonSettings = Seq(
  sonatypeProfileName := sonatypeProfile,
  mimaPreviousArtifacts := previousVersion.map(organization.value %% moduleName.value % _).toSet,
  mimaBinaryIssueFilters ++= Seq(
  )
)

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

ThisBuild / description := "Authentication library for Play Framework applications that supports several authentication methods, including OAuth1, OAuth2, OpenID, CAS, Credentials, Basic Authentication, Two Factor Authentication or custom authentication schemes"
ThisBuild / homepage := Some(url("https://silhouette.readme.io/"))
ThisBuild / licenses := Seq("Apache License" -> url("https://github.com/playframework/play-silhouette/blob/main/LICENSE"))
ThisBuild / Test / publishArtifact := false
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / organization := "org.playframework.silhouette"
ThisBuild / organizationName := "The Play Framework Project"
ThisBuild / scalaVersion := scala213
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-Xfatal-warnings"
) ++
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Seq(
      "-encoding", "utf8",
      "-unchecked",
      "-deprecation",
      "-Xlint:adapted-args",
      "-Xlint:inaccessible",
      "-Xlint:infer-any",
      "-Xlint:nullary-unit"
    )
    case _ => Seq()
  })
ThisBuild / Test / scalacOptions ~= { options: Seq[String] =>
  // Allow dead code in tests (to support using mockito).
  options filterNot (_ == "-Ywarn-dead-code")
}
ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / crossVersion := CrossVersion.full
ThisBuild / Test / parallelExecution := false
ThisBuild / Test / fork := true
ThisBuild / javaOptions += "-Xmx1G"

ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible

ThisBuild / developers ++= List(
  Developer(
    "ndeverge",
    "Nicolas Deverge",
    "ndeverge",
    url("https://github.com/ndeverge")
  ),
  Developer(
    "MathisGuillet1",
    "Mathis Guillet",
    "MathisGuillet1",
    url("https://github.com/MathisGuillet1")
  ),
)

lazy val root = (project in file("."))
  .disablePlugins(MimaPlugin)
  .aggregate(
    silhouette,
    silhouetteCas,
    silhouetteTotp,
    silhouetteCryptoJca,
    silhouetteArgon2,
    silhouetteBcrypt,
    silhouettePersistence,
    silhouetteTestkit
  )
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    name := "play-silhouette-root",
    sonatypeProfileName := sonatypeProfile,
    Defaults.coreDefaultSettings,
    publish / skip := true,
  )

lazy val silhouette = (project in file("silhouette"))
  .settings(commonSettings)
  .settings(
    name := "play-silhouette",
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.Play.cache,
        Library.Play.ws,
        Library.Play.openid,
        Library.jwt,
        Library.apacheCommonLang,
        Library.Play.specs2 % Test,
        Library.Specs2.matcherExtra % Test,
        Library.mockito % Test,
        Library.scalaGuice % Test,
        Library.pekkoTestkit % Test
      ),
  )
  .enablePlugins(PlayScala)
  .disablePlugins(PlayPekkoHttpServer)

lazy val silhouetteCas = (project in file("silhouette-cas"))
  .settings(commonSettings)
  .settings(
    name := "play-silhouette-cas",
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.casClient,
        Library.casClientSupportSAML,
        Library.Play.specs2 % Test,
        Library.Specs2.matcherExtra % Test,
        Library.mockito % Test,
        Library.scalaGuice % Test
      )
  )
  .dependsOn(silhouette % "compile->compile;test->test")

lazy val silhouetteTotp = (project in file("silhouette-totp"))
  .settings(commonSettings)
  .settings(
    name := "play-silhouette-totp",
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.googleAuth,
        Library.Play.specs2 % Test
      )
  )
  .dependsOn(silhouette % "compile->compile;test->test")

lazy val silhouetteCryptoJca = (project in file("silhouette-crypto-jca"))
  .settings(commonSettings)
  .settings(
    name := "play-silhouette-crypto-jca",
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.commonsCodec,
        Library.Specs2.core % Test,
        Library.Specs2.matcherExtra % Test
      )
  )
  .dependsOn(silhouette)

lazy val silhouetteArgon2 = (project in file("silhouette-password-argon2"))
  .settings(commonSettings)
  .settings(
    name := "play-silhouette-password-argon2",
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.argon2,
        Library.Specs2.core % Test
      )
  )
  .dependsOn(silhouette)

lazy val silhouetteBcrypt = (project in file("silhouette-password-bcrypt"))
  .settings(commonSettings)
  .settings(
    name := "play-silhouette-password-bcrypt",
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.jbcrypt,
        Library.Specs2.core % Test
      )
  )
  .dependsOn(silhouette)

lazy val silhouettePersistence = (project in file("silhouette-persistence"))
  .settings(commonSettings)
  .settings(
    name := "play-silhouette-persistence",
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.Specs2.core % Test,
        Library.Specs2.matcherExtra % Test,
        Library.mockito % Test,
        Library.scalaGuice % Test
      )
  )
  .dependsOn(silhouette)

lazy val silhouetteTestkit = (project in file("silhouette-testkit"))
  .settings(commonSettings)
  .settings(
    name := "play-silhouette-testkit",
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.Play.test,
        Library.Play.specs2 % Test,
        Library.Specs2.matcherExtra % Test,
        Library.mockito % Test,
        Library.scalaGuice % Test,
        Library.pekkoTestkit % Test
      )
      ++ {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((3, _)) => Seq(Library.izumiReflect)
          case _ => Seq.empty
        }
      }
  )
  .enablePlugins(PlayScala)
  .dependsOn(silhouette)
