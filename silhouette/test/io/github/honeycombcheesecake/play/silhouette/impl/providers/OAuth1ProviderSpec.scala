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
package io.github.honeycombcheesecake.play.silhouette.impl.providers

import io.github.honeycombcheesecake.play.silhouette.api.util.MockHTTPLayer
import io.github.honeycombcheesecake.play.silhouette.impl.exceptions.{ AccessDeniedException, UnexpectedResponseException }
import io.github.honeycombcheesecake.play.silhouette.impl.providers.OAuth1Provider._
import io.github.honeycombcheesecake.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import org.specs2.matcher.ThrownExpectations
import org.specs2.specification.Scope
import play.api.mvc.{ AnyContent, AnyContentAsEmpty, Result, Results }
import play.api.test.{ FakeHeaders, FakeRequest, WithApplication }
import play.mvc.Http.HeaderNames
import org.hamcrest.core.IsAnything
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.mockito.hamcrest.MockitoHamcrest
import test.SocialProviderSpec
import test.Helper.mock

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Abstract test case for the [[OAuth1Provider]] class.
 *
 * These tests will be additionally executed before every OAuth1 provider spec.
 */
abstract class OAuth1ProviderSpec extends SocialProviderSpec[OAuth1Info] {
  isolated

  "The provider" should {
    val c = context
    "throw a RuntimeException if the unsafe 1.0 specification should be used" in new WithApplication {
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "?" + Denied + "=")
      when(c.oAuthService.use10a).thenReturn(false)
      c.provider.authenticate() must throwA[RuntimeException]
    }
  }

  "The authenticate method" should {
    val c = context
    "fail with an AccessDeniedException if denied key exists in query string" in new WithApplication {
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "?" + Denied + "=")
      failed[AccessDeniedException](c.provider.authenticate()) {
        case e => e.getMessage must startWith(AuthorizationError.format(c.provider.id, ""))
      }
    }

    "fail with an UnexpectedResponseException if request token cannot be retrieved" in new WithApplication {
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      when(c.oAuthService.retrieveRequestToken(c.oAuthSettings.callbackURL)).thenReturn(Future.failed(new Exception("")))

      failed[UnexpectedResponseException](c.provider.authenticate()) {
        case e => e.getMessage must startWith(ErrorRequestToken.format(c.provider.id, ""))
      }
    }

    "redirect to authorization URL if request token could be retrieved" in new WithApplication {
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val serializedTokenSecret = "my.serialized.token.secret"

      when(c.oAuthService.retrieveRequestToken(c.oAuthSettings.callbackURL)).thenReturn(Future.successful(c.oAuthInfo))
      when(c.oAuthService.redirectUrl(any())).thenAnswer(_ => c.oAuthSettings.authorizationURL)
      when(c.oAuthTokenSecretProvider.build(any())(any(), any())).thenReturn(Future.successful(c.oAuthTokenSecret))
      // when(c.oAuthTokenSecretProvider.publish(any(), any())(any())).thenAnswer(_.getArguments.asInstanceOf[Array[Any]](0).asInstanceOf[Result])
      when(c.oAuthTokenSecretProvider.publish(any(), any())(any())).thenAnswer(_.getArgument(0).asInstanceOf[Result])

      result(c.provider.authenticate()) { result =>
        status(result) must equalTo(SEE_OTHER)
        redirectLocation(result) must beSome[String].which(_ == c.oAuthSettings.authorizationURL)
      }
    }

    "resolves relative redirectURLs before starting the flow" in new WithApplication {
      verifyCallbackURLResolution("/callback-url", secure = false, "http://www.example.com/callback-url")
    }

    "resolves path relative redirectURLS before starting the flow" in new WithApplication {
      verifyCallbackURLResolution("callback-url", secure = false, "http://www.example.com/request-path/callback-url")
    }

    def verifyCallbackURLResolution(callbackURL: String, secure: Boolean, resolvedCallbackURL: String) = {
      implicit val req = FakeRequest[AnyContent](
        method = GET,
        uri = "/request-path/something",
        headers = FakeHeaders(Seq((HeaderNames.HOST, "www.example.com"))),
        body = AnyContentAsEmpty,
        secure = secure)

      when(c.oAuthSettings.callbackURL).thenReturn(callbackURL)

      when(c.oAuthService.retrieveRequestToken(any())(any())).thenReturn(Future.successful(c.oAuthInfo))

      when(c.oAuthService.redirectUrl(c.oAuthInfo.token)).thenAnswer(_ => c.oAuthSettings.authorizationURL)

      when(c.oAuthTokenSecretProvider.build(any())(any(), any())).thenReturn(Future.successful(c.oAuthTokenSecret))
      when(c.oAuthTokenSecretProvider.publish(any(), any())(any())).thenAnswer(_ => Results.Redirect(c.oAuthSettings.authorizationURL))

      await(c.provider.authenticate())
      verify(c.oAuthService).retrieveRequestToken(resolvedCallbackURL)
    }

    "resolves relative redirectURLs before starting the flow over https" in new WithApplication {
      verifyCallbackURLResolution("/callback-url", secure = true, "https://www.example.com/callback-url")
    }

    "fail with an UnexpectedResponseException if access token cannot be retrieved" in new WithApplication {
      val tokenSecret = "my.token.secret"
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "?" + OAuthVerifier + "=my.verifier&" + OAuthToken + "=my.token")

      when(c.oAuthTokenSecret.value).thenReturn(tokenSecret)
      when(c.oAuthTokenSecretProvider.retrieve(any(), any())).thenReturn(Future.successful(c.oAuthTokenSecret))
      when(c.oAuthService.retrieveAccessToken(c.oAuthInfo.copy(secret = tokenSecret), "my.verifier")).thenReturn(Future.failed(new Exception("")))

      failed[UnexpectedResponseException](c.provider.authenticate()) {
        case e => e.getMessage must startWith(ErrorAccessToken.format(c.provider.id, ""))
      }
    }

    "return the auth info" in new WithApplication {
      val tokenSecret = "my.token.secret"
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "?" + OAuthVerifier + "=my.verifier&" + OAuthToken + "=my.token")

      when(c.oAuthTokenSecret.value).thenReturn(tokenSecret)
      when(c.oAuthTokenSecretProvider.retrieve(any(), any())).thenReturn(Future.successful(c.oAuthTokenSecret))
      when(c.oAuthService.retrieveAccessToken(c.oAuthInfo.copy(secret = tokenSecret), "my.verifier")).thenReturn(Future.successful(c.oAuthInfo))

      authInfo(c.provider.authenticate())(_ must be equalTo c.oAuthInfo)
    }
  }

  "The `settings` method" should {
    val c = context
    "return the settings instance" in {
      c.provider.settings must be equalTo c.oAuthSettings
    }
  }

  /**
   * Defines the context for the abstract OAuth1 provider spec.
   *
   * @return The Context to use for the abstract OAuth1 provider spec.
   */
  protected def context: OAuth1ProviderSpecContext
}

/**
 * Context for the OAuth1ProviderSpec.
 */
trait OAuth1ProviderSpecContext extends Scope with ThrownExpectations {

  abstract class TestSecret extends OAuth1TokenSecret
  abstract class TestStateProvider extends OAuth1TokenSecretProvider {
    type Secret = TestSecret
  }

  /**
   * The HTTP layer mock.
   */
  lazy val httpLayer = {
    val m = mock[MockHTTPLayer]
    when(m.executionContext).thenReturn(global)
    m
  }

  /**
   * A OAuth1 info.
   */
  lazy val oAuthInfo = OAuth1Info("my.token", "my.consumer.secret")

  /**
   * The OAuth1 service mock.
   */
  lazy val oAuthService: OAuth1Service = {

    val s = mock[PlayOAuth1Service]
    when(s.use10a).thenReturn(true)
    when(s.withSettings(MockitoHamcrest.argThat(new IsAnything[OAuth1Settings => OAuth1Settings]))).thenReturn(s)
    when(s.redirectUrl(anyString)).thenReturn("TESTING")
    s
  }

  /**
   * A OAuth1 token secret.
   */
  lazy val oAuthTokenSecret: TestSecret = mock[TestSecret]

  /**
   * The OAuth1 token secret provider mock.
   */
  lazy val oAuthTokenSecretProvider: TestStateProvider = mock[TestStateProvider]

  /**
   * The OAuth1 settings.
   */
  def oAuthSettings: OAuth1Settings

  /**
   * The provider to test.
   */
  def provider: OAuth1Provider
}
