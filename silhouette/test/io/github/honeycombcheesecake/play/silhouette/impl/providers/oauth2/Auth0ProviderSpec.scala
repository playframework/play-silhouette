/**
 * Copyright 2015 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.honeycombcheesecake.play.silhouette.impl.providers.oauth2

import io.github.honeycombcheesecake.play.silhouette.api.LoginInfo
import io.github.honeycombcheesecake.play.silhouette.api.util.{ ExtractableRequest, MockWSRequest }
import io.github.honeycombcheesecake.play.silhouette.impl.exceptions.{ ProfileRetrievalException, UnexpectedResponseException }
import io.github.honeycombcheesecake.play.silhouette.impl.providers.OAuth2Provider._
import io.github.honeycombcheesecake.play.silhouette.impl.providers._
import io.github.honeycombcheesecake.play.silhouette.impl.providers.oauth2.Auth0Provider._
import play.api.mvc.AnyContentAsEmpty
import play.api.libs.json.Json
import play.api.test.{ FakeRequest, WithApplication }
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import test.Helper
import test.Helper.mock

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Test case for the [[Auth0Provider]] class.
 */
class Auth0ProviderSpec extends OAuth2ProviderSpec {

  "The `withSettings` method" should {
    "create a new instance with customized settings" in new WithApplication with Context {
      val s: Auth0Provider = provider.withSettings { s =>
        s.copy(accessTokenURL = "new-access-token-url")
      }

      s.settings.accessTokenURL must be equalTo "new-access-token-url"
    }
  }

  "The `authenticate` method" should {
    "fail with UnexpectedResponseException for an unexpected response" in new WithApplication with Context {
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "?" + Code + "=my.code")
      when(wsResponse.status).thenReturn(401)
      when(wsResponse.body).thenReturn("Unauthorized")
      when(wsRequest.withHttpHeaders(any)).thenReturn(wsRequest)
      when(wsRequest.post[Map[String, Seq[String]]](any)(any)).thenReturn(Future.successful(wsResponse))
      when(httpLayer.url(oAuthSettings.accessTokenURL)).thenReturn(wsRequest)
      when(stateProvider.unserialize(anyString)(any[ExtractableRequest[String]], any[ExecutionContext])).thenReturn(Future.successful(state))
      when(stateProvider.state(any[ExecutionContext])).thenReturn(Future.successful(state))

      failed[UnexpectedResponseException](provider.authenticate()) {
        case e => e.getMessage must startWith(UnexpectedResponse.format(provider.id, "Unauthorized", 401))
      }
    }

    "fail with UnexpectedResponseException if OAuth2Info can be build because of an unexpected response" in new WithApplication with Context {
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "?" + Code + "=my.code")

      when(wsResponse.status).thenReturn(200)
      when(wsResponse.json).thenReturn(Json.obj())
      when(wsRequest.withHttpHeaders(any)).thenReturn(wsRequest)
      when(wsRequest.post[Map[String, Seq[String]]](any)(any)).thenReturn(Future.successful(wsResponse))
      when(httpLayer.url(oAuthSettings.accessTokenURL)).thenReturn(wsRequest)
      when(stateProvider.unserialize(anyString)(any[ExtractableRequest[String]], any[ExecutionContext])).thenReturn(Future.successful(state))
      when(stateProvider.state(any[ExecutionContext])).thenReturn(Future.successful(state))

      failed[UnexpectedResponseException](provider.authenticate()) {
        case e => e.getMessage must startWith(InvalidInfoFormat.format(provider.id, ""))
      }
    }

    "return the auth info" in new WithApplication with Context {
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "?" + Code + "=my.code")

      when(wsResponse.status).thenReturn(200)
      when(wsResponse.json).thenReturn(oAuthInfo)
      when(wsRequest.withHttpHeaders(any)).thenReturn(wsRequest)
      when(wsRequest.post[Map[String, Seq[String]]](any)(any)).thenReturn(Future.successful(wsResponse))
      when(httpLayer.url(oAuthSettings.accessTokenURL)).thenReturn(wsRequest)
      when(stateProvider.unserialize(anyString)(any[ExtractableRequest[String]], any[ExecutionContext])).thenReturn(Future.successful(state))
      when(stateProvider.state(any[ExecutionContext])).thenReturn(Future.successful(state))

      authInfo(provider.authenticate())(_ must be equalTo oAuthInfo.as[OAuth2Info])
    }
  }

  "The `authenticate` method with user state" should {
    "return stateful auth info" in new WithApplication with Context {
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]
      implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "?" + Code + "=my.code")
      when(wsResponse.status).thenReturn(200)
      when(wsResponse.json).thenReturn(oAuthInfo)
      when(wsRequest.withHttpHeaders(any)).thenReturn(wsRequest)
      when(wsRequest.post[Map[String, Seq[String]]](any)(any)).thenReturn(Future.successful(wsResponse))
      when(httpLayer.url(oAuthSettings.accessTokenURL)).thenReturn(wsRequest)
      when(stateProvider.unserialize(anyString)(any[ExtractableRequest[String]], any[ExecutionContext])).thenReturn(Future.successful(state))
      when(stateProvider.state(any[ExecutionContext])).thenReturn(Future.successful(state))
      when(stateProvider.withHandler(any[SocialStateItemHandler])).thenReturn(stateProvider)
      when(state.items).thenReturn(Set(userStateItem))

      statefulAuthInfo(provider.authenticate(userStateItem))(_ must be equalTo stateAuthInfo)
    }
  }

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new WithApplication with Context {
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]

      when(wsResponse.status).thenReturn(400)
      when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
      when(wsRequest.withHttpHeaders(("Authorization", s"Bearer ${oAuthInfoObject.accessToken}"))).thenReturn(wsRequest)
      when(httpLayer.url(oAuthSettings.apiURL.get)).thenReturn(wsRequest)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuthInfoObject)) {
        case e => e.getMessage must equalTo(GenericHttpStatusProfileError.format(provider.id, 400))
      }
    }

    "fail with ProfileRetrievalException if an unexpected error occurred" in new WithApplication with Context {
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]

      when(wsResponse.status).thenReturn(500)
      when(wsResponse.json).thenThrow(new RuntimeException(""))
      when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
      when(wsRequest.withHttpHeaders(("Authorization", s"Bearer ${oAuthInfoObject.accessToken}"))).thenReturn(wsRequest)
      when(httpLayer.url(oAuthSettings.apiURL.get)).thenReturn(wsRequest)

      failed[ProfileRetrievalException](provider.retrieveProfile(oAuthInfo.as[OAuth2Info])) {
        case e => e.getMessage must equalTo("[Silhouette][auth0] error retrieving profile information")
      }
    }

    "return the social profile" in new WithApplication with Context {
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]
      val userProfile = Helper.loadJson(Auth0UserProfileJson)

      when(wsResponse.status).thenReturn(200)
      when(wsResponse.json).thenReturn(userProfile)
      when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
      when(wsRequest.withHttpHeaders(("Authorization", s"Bearer ${oAuthInfoObject.accessToken}"))).thenReturn(wsRequest)
      when(httpLayer.url(oAuthSettings.apiURL.get)).thenReturn(wsRequest)

      profile(provider.retrieveProfile(oAuthInfo.as[OAuth2Info])) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, (userProfile \ "sub").as[String]),
          fullName = (userProfile \ "name").asOpt[String],
          email = (userProfile \ "email").asOpt[String],
          avatarURL = (userProfile \ "picture").asOpt[String])
      }
    }
  }

  /**
   * Defines the context for the abstract OAuth2 provider spec.
   *
   * @return The Context to use for the abstract OAuth2 provider spec.
   */
  override protected def context: OAuth2ProviderSpecContext = new Context {}

  /**
   * The context.
   */
  trait Context extends OAuth2ProviderSpecContext {

    /**
     * Paths to the Json fixtures.
     */
    val Auth0ErrorJson = "providers/custom/auth0.error.json"
    val Auth0SuccessJson = "providers/custom/auth0.success.json"
    val Auth0UserProfileJson = "providers/custom/auth0.profile.json"

    /**
     * The OAuth2 settings.
     */
    override lazy val oAuthSettings = spy(OAuth2Settings(
      authorizationURL = Some("https://gerritforge.eu.auth0.com/authorize"),
      accessTokenURL = "https://gerritforge.eu.auth0.com/oauth/token",
      apiURL = Some("https://gerritforge.eu.auth0.com/userinfo"),
      redirectURL = Some("https://www.mohiva.com"),
      clientID = "some.client.id",
      clientSecret = "some.secret",
      scope = Some("email")))

    /**
     * The OAuth2 info returned by Auth0.
     */
    override lazy val oAuthInfo = Helper.loadJson(Auth0SuccessJson)

    /**
     * The OAuth2 info deserialized as case class object
     */
    lazy val oAuthInfoObject = oAuthInfo.as[OAuth2Info]

    /**
     * The stateful auth info.
     */
    override lazy val stateAuthInfo = StatefulAuthInfo(oAuthInfoObject, userStateItem)

    /**
     * The provider to test.
     */
    lazy val provider: Auth0Provider = new Auth0Provider(httpLayer, stateProvider, oAuthSettings)
  }
}
