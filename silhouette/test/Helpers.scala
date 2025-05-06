/**
 * Copyright 2015 Mohiva Organisation (license at mohiva dot com)
 *
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
package test

import play.silhouette.api.AuthInfo
import play.silhouette.impl.providers.{ SocialProfile, SocialStateItem, StatefulAuthInfo }
import org.specs2.execute.{ AsResult, Result => Specs2Result }
import org.specs2.matcher.{ JsonMatchers, MatchResult }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Result => PlayResult }
import play.api.test.PlaySpecification

import scala.concurrent.Future
import scala.io.{ Codec, Source }
import scala.reflect.ClassTag
import org.mockito.Mockito

/**
 * Base test case for the social providers.
 */
trait SocialProviderSpec[A <: AuthInfo] extends PlaySpecification with JsonMatchers {

  /**
   * Applies a matcher on a simple result.
   *
   * @param providerResult The result from the provider.
   * @param b The matcher block to apply.
   * @return A specs2 match result.
   */
  def result(providerResult: Future[Either[PlayResult, A]])(b: Future[PlayResult] => MatchResult[?]) = {
    await(providerResult) must beLeft[PlayResult].like {
      case result => b(Future.successful(result))
    }
  }

  /**
   * Applies a matcher on a auth info.
   *
   * @param providerResult The result from the provider.
   * @param b The matcher block to apply.
   * @return A specs2 match result.
   */
  def authInfo(providerResult: Future[Either[PlayResult, A]])(b: A => MatchResult[?]) = {
    await(providerResult) must beRight[A].like {
      case authInfo => b(authInfo)
    }
  }

  /**
   * Applies a matcher on a social profile.
   *
   * @param providerResult The result from the provider.
   * @param b The matcher block to apply.
   * @return A specs2 match result.
   */
  def profile(providerResult: Future[SocialProfile])(b: SocialProfile => MatchResult[?]) = {
    await(providerResult) must beLike[SocialProfile] {
      case socialProfile => b(socialProfile)
    }
  }

  /**
   * Matches a partial function against a failure message.
   *
   * This method checks if an exception was thrown in a future.
   * @see https://groups.google.com/d/msg/specs2-users/MhJxnvyS1_Q/FgAK-5IIIhUJ
   *
   * @param providerResult The result from the provider.
   * @param f A matcher function.
   * @return A specs2 match result.
   */
  def failed[E <: Throwable: ClassTag](providerResult: Future[?])(f: => PartialFunction[Throwable, MatchResult[?]]) = {
    implicit class Rethrow(t: Throwable) {
      def rethrow = { throw t; t }
    }

    lazy val result = await(providerResult.failed)

    result must not[Throwable](throwAn[E])
    result.rethrow must throwAn[E].like(f)
  }
}

/**
 * Base test case for the social state providers.
 */
trait SocialStateProviderSpec[A <: AuthInfo, S <: SocialStateItem] extends SocialProviderSpec[A] {

  /**
   * Applies a matcher on a simple result.
   *
   * @param providerResult The result from the provider.
   * @param b              The matcher block to apply.
   * @return A specs2 match result.
   */
  def statefulResult(providerResult: Future[Either[PlayResult, StatefulAuthInfo[A, S]]])(
    b: Future[PlayResult] => MatchResult[?]) = {
    await(providerResult) must beLeft[PlayResult].like {
      case result => b(Future.successful(result))
    }
  }

  /**
   * Applies a matcher on a stateful auth info.
   *
   * @param providerResult The result from the provider.
   * @param b              The matcher block to apply.
   * @return A specs2 match result.
   */
  def statefulAuthInfo(providerResult: Future[Either[PlayResult, StatefulAuthInfo[A, S]]])(
    b: StatefulAuthInfo[A, S] => MatchResult[?]) = {
    await(providerResult) must beRight[StatefulAuthInfo[A, S]].like {
      case info => b(info)
    }
  }
}

/**
 * Some test-related helper methods.
 */
object Helper {

  /**
   * Loads a JSON file from class path.
   *
   * @param file The file to load.
   * @return The JSON value.
   */
  def loadJson(file: String): JsValue = {
    Option(this.getClass.getResourceAsStream("/" + file.stripPrefix("/"))) match {
      case Some(is) => Json.parse(Source.fromInputStream(is)(Codec.UTF8).mkString)
      case None => throw new Exception("Cannot load file: " + file)
    }
  }

  /**
   * Mock related helpers
   */

  def mock[A](implicit a: ClassTag[A]): A =
    Mockito.mock(a.runtimeClass).asInstanceOf[A]

  def mockSmart[A](implicit a: ClassTag[A]): A =
    Mockito.mock(a.runtimeClass, Mockito.withSettings().defaultAnswer(Mockito.RETURNS_SMART_NULLS)).asInstanceOf[A]

}
