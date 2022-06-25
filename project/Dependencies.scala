/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import sbt._

object Dependencies {

  val resolvers: Seq[MavenRepository] = Seq[MavenRepository](elems =
    "Atlassian Releases" at "https://packages.atlassian.com/public/"
  )

  object Library {

    val updates: Seq[ModuleID] = Seq(
      "commons-io" % "commons-io" % "2.11.0"
    )

    object Play {
      val version: String = play.core.PlayVersion.current
      val ws = "com.typesafe.play" %% "play-ws" % version
      val cache = "com.typesafe.play" %% "play-cache" % version
      val test = "com.typesafe.play" %% "play-test" % version
      val specs2 = "com.typesafe.play" %% "play-specs2" % version
      val openid = "com.typesafe.play" %% "play-openid" % version
    }

    object Specs2 {
      private val version = "4.9.4"  // Versions later than this will fail due to removed dependencies.
      val core = "org.specs2" %% "specs2-core" % version
      val matcherExtra = "org.specs2" %% "specs2-matcher-extra" % version
      val mock = "org.specs2" %% "specs2-mock" % version
    }

    val argon2 = "de.mkammerer" % "argon2-jvm" % "2.11"
    val commonsCodec = "commons-codec" % "commons-codec" % "1.15"
    val jbcrypt = "de.svenkubiak" % "jBCrypt" % "0.4.3"
    val jwt = "com.auth0" % "java-jwt" % "3.18.2"
    val scalaGuice = "net.codingwell" %% "scala-guice" % "5.1.0"
    val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % play.core.PlayVersion.akkaVersion
    val casClient = "org.jasig.cas.client" % "cas-client-core" % "3.6.4"
    val casClientSupportSAML = "org.jasig.cas.client" % "cas-client-support-saml" % "3.6.4"
    val apacheCommonLang = "org.apache.commons" % "commons-lang3" % "3.12.0"
    val googleAuth = "com.warrenstrange" % "googleauth" % "1.5.0"
  }
}
