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

  object Library {

    val updates: Seq[ModuleID] = Seq(
      "commons-io" % "commons-io" % "2.15.1"
    )

    object Play {
      val version: String = play.core.PlayVersion.current
      val ws = "org.playframework" %% "play-ws" % version
      val cache = "org.playframework" %% "play-cache" % version
      val test = "org.playframework" %% "play-test" % version
      val specs2 = "org.playframework" %% "play-specs2" % version
      val openid = "org.playframework" %% "play-openid" % version
    }

    object Specs2 {
      private val version = "4.20.5"
      val core = "org.specs2" %% "specs2-core" % version
      val matcherExtra = "org.specs2" %% "specs2-matcher-extra" % version
    }

    val argon2 = "de.mkammerer" % "argon2-jvm" % "2.11"
    val commonsCodec = "commons-codec" % "commons-codec" % "1.16.0"
    val jbcrypt = "de.svenkubiak" % "jBCrypt" % "0.4.3"
    val jwt = "com.auth0" % "java-jwt" % "3.19.4"
    val scalaGuice = "net.codingwell" %% "scala-guice" % "6.0.0"
    val pekkoTestkit = "org.apache.pekko" %% "pekko-testkit" % play.core.PlayVersion.pekkoVersion
    val mockito = "org.mockito" % "mockito-core" % "5.9.0"
    val casClient = "org.jasig.cas.client" % "cas-client-core" % "3.6.4"
    val casClientSupportSAML = "org.jasig.cas.client" % "cas-client-support-saml" % "3.6.4"
    val apacheCommonLang = "org.apache.commons" % "commons-lang3" % "3.14.0"
    val googleAuth = "com.warrenstrange" % "googleauth" % "1.5.0"
    val izumiReflect = "dev.zio" %% "izumi-reflect" % "2.3.8" // Scala 3 replacement for scala 2 reflect universe
  }
}
