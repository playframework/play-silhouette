import Dependencies.Library
import sbt.CrossVersion

lazy val repo: String = "https://s01.oss.sonatype.org"
lazy val scala213: String = "2.13.12"
lazy val scala3: String = "3.3.1"
lazy val supportedScalaVersions: Seq[String] = Seq(scala213, scala3)

Global / evictionErrorLevel   := Level.Info

ThisBuild / description := "Authentication library for Play Framework applications that supports several authentication methods, including OAuth1, OAuth2, OpenID, CAS, Credentials, Basic Authentication, Two Factor Authentication or custom authentication schemes"
ThisBuild / homepage := Some(url("https://silhouette.readme.io/"))
ThisBuild / licenses := Seq("Apache License" -> url("https://github.com/honeycomb-cheesecake/play-silhouette/blob/main/LICENSE"))
ThisBuild / publishMavenStyle := true
ThisBuild / Test / publishArtifact := false
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / organization := "io.github.honeycomb-cheesecake"
ThisBuild / organizationName := "honeycomb-cheesecake"
ThisBuild / scalaVersion := scala213
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / scalacOptions ++= Seq(
  //"-unchecked",
  //"-deprecation",
  "-feature",
  //"-encoding", "utf8",
  "-Xfatal-warnings",
  //"-Xlint",
  //"-Xlint:adapted-args",
  //"-Xlint:inaccessible",
  //"-Xlint:infer-any",
  //"-Xlint:nullary-unit"
)
ThisBuild / Test / scalacOptions ~= { options: Seq[String] =>
  // Allow dead code in tests (to support using mockito).
  options filterNot (_ == "-Ywarn-dead-code")
}
ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / crossVersion := CrossVersion.full
ThisBuild / Test / parallelExecution := false
ThisBuild / Test / fork := true
ThisBuild / javaOptions += "-Xmx1G"

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

ThisBuild / publishTo := {
  if(isSnapshot.value) {
    Some("Sonatype Nexus Repository Manager" at s"$repo/content/repositories/snapshots/")
  }
  else {
    sonatypePublishToBundle.value
  }
}
ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible

dependencyCheckAssemblyAnalyzerEnabled := Some(false)
dependencyCheckFormat := "ALL"
dependencyCheckSkipTestScope := true
dependencyCheckSuppressionFiles := Seq[sbt.File](new sbt.File("dependency-check-suppression.xml"))

ThisBuild / pomExtra := {
  <scm>
    <url>git@github.com:honeycomb-cheesecake/play-silhouette.git</url>
    <connection>scm:git:git@github.com:honeycomb-cheesecake/play-silhouette.git</connection>
  </scm>
    <developers>
      <developer>
        <id>honeycomb-cheesecake</id>
        <name>Simon Ramzi</name>
        <url>https://github.com/honeycomb-cheesecake</url>
      </developer>
      <developer>
        <id>ndeverge</id>
        <name>Nicolas Deverge</name>
        <url>https://github.com/ndeverge</url>
      </developer>
    </developers>
}

lazy val root = (project in file("."))
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
    Defaults.coreDefaultSettings,
    publish / skip := true,
    publishLocal := {},
    publishM2 := {},
    publishArtifact := false
  )

lazy val silhouette = (project in file("silhouette"))
  .settings(
    name := "play-silhouette",
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-matcher-extra"),
    dependencyUpdatesFilter -= moduleFilter(organization = "com.typesafe.akka", name = "akka-testkit"),
    dependencyUpdatesFilter -= moduleFilter(organization = "commons-io", name = "commons-io"),
    dependencyUpdatesFilter -= moduleFilter(organization = "om.auth0", name = "java-jwt"),
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
        Library.akkaTestkit % Test
      ),
    resolvers ++= Dependencies.resolvers
  )
  .enablePlugins(PlayScala)
  .disablePlugins(PlayAkkaHttpServer)

lazy val silhouetteCas = (project in file("silhouette-cas"))
  .settings(
    name := "play-silhouette-cas",
    dependencyUpdatesFailBuild := false,
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-matcher-extra"),
    dependencyUpdatesFilter -= moduleFilter(organization = "commons-io", name = "commons-io"),
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
  .settings(
    name := "play-silhouette-totp",
    dependencyUpdatesFailBuild := false,
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-core"),
    dependencyUpdatesFilter -= moduleFilter(organization = "commons-io", name = "commons-io"),
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.googleAuth,
        Library.Play.specs2 % Test
      )
  )
  .dependsOn(silhouette % "compile->compile;test->test")

lazy val silhouetteCryptoJca = (project in file("silhouette-crypto-jca"))
  .settings(
    name := "play-silhouette-crypto-jca",
    dependencyUpdatesFailBuild := false,
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-core"),
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-matcher-extra"),
    dependencyUpdatesFilter -= moduleFilter(organization = "commons-io", name = "commons-io"),
    dependencyUpdatesFilter -= moduleFilter(organization = "commons-codec", name = "commons-codec"),

    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.commonsCodec,
        Library.Specs2.core % Test,
        Library.Specs2.matcherExtra % Test
      )
  )
  .dependsOn(silhouette)

lazy val silhouetteArgon2 = (project in file("silhouette-password-argon2"))
  .settings(
    name := "play-silhouette-password-argon2",
    dependencyUpdatesFailBuild := false,
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-core"),
    dependencyUpdatesFilter -= moduleFilter(organization = "commons-io", name = "commons-io"),
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.argon2,
        Library.Specs2.core % Test
      )
  )
  .dependsOn(silhouette)

lazy val silhouetteBcrypt = (project in file("silhouette-password-bcrypt"))
  .settings(
    name := "play-silhouette-password-bcrypt",
    dependencyUpdatesFailBuild := false,
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-core"),
    dependencyUpdatesFilter -= moduleFilter(organization = "commons-io", name = "commons-io"),
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.jbcrypt,
        Library.Specs2.core % Test
      )
  )
  .dependsOn(silhouette)

lazy val silhouettePersistence = (project in file("silhouette-persistence"))
  .settings(
    name := "play-silhouette-persistence",
    dependencyUpdatesFailBuild := false,
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-core"),
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-matcher-extra"),
    dependencyUpdatesFilter -= moduleFilter(organization = "commons-io", name = "commons-io"),
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
  .settings(
    name := "play-silhouette-testkit",
    dependencyUpdatesFailBuild := false,
    dependencyUpdatesFilter -= moduleFilter(organization = "org.specs2", name = "specs2-matcher-extra"),
    dependencyUpdatesFilter -= moduleFilter(organization = "com.typesafe.akka", name = "akka-testkit"),
    dependencyUpdatesFilter -= moduleFilter(organization = "commons-io", name = "commons-io"),
    libraryDependencies ++=
      Library.updates ++ Seq(
        Library.Play.test,
        Library.Play.specs2 % Test,
        Library.Specs2.matcherExtra % Test,
        Library.mockito % Test,
        Library.scalaGuice % Test,
        Library.akkaTestkit % Test
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

import ReleaseTransformations._
releaseTagComment        := s"Releasing ${(ThisBuild / version).value}"
releaseCommitMessage     := s"Releasing ${(ThisBuild / version).value}"
releaseCrossBuild        := true
releaseNextCommitMessage := s"Setting version to ${(ThisBuild / version).value}"
releaseProcess := Seq[ReleaseStep](
  runClean,
  releaseStepTask(dependencyCheckAggregate),
  releaseStepTask(dependencyUpdates),
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  releaseStepTask(versionCheck),
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
