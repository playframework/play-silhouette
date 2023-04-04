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
package io.github.honeycombcheesecake.play.silhouette.impl.providers.oauth2

import io.github.honeycombcheesecake.play.silhouette.api.LoginInfo
import io.github.honeycombcheesecake.play.silhouette.api.util.{ ExtractableRequest, MockHTTPLayer, MockWSRequest }
import io.github.honeycombcheesecake.play.silhouette.impl.exceptions.{ ProfileRetrievalException, UnexpectedResponseException }
import io.github.honeycombcheesecake.play.silhouette.impl.providers.OAuth2Provider._
import io.github.honeycombcheesecake.play.silhouette.impl.providers.SocialProfileBuilder._
import io.github.honeycombcheesecake.play.silhouette.impl.providers._
import io.github.honeycombcheesecake.play.silhouette.impl.providers.oauth2.LinkedInProvider._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{ FakeRequest, WithApplication }
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import test.Helper
import test.Helper.mock

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Test case for the [[LinkedInProvider]] class.
 */
class LinkedInProviderSpec extends OAuth2ProviderSpec {

  "The `withSettings` method" should {
    "create a new instance with customized settings" in new WithApplication with Context {
      val s = provider.withSettings { s =>
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
      when(wsResponse.status).thenReturn(401)
      when(wsResponse.json).thenReturn(Helper.loadJson("providers/oauth2/linkedin.error.json"))
      when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
      when(httpLayer.url(API.format("my.access.token"))).thenReturn(wsRequest)
      mockEmailAndPhoto(httpLayer)
      failed[ProfileRetrievalException](provider.retrieveProfile(oAuthInfo.as[OAuth2Info])) {
        case e => e.getMessage must equalTo(SpecifiedProfileError.format(
          provider.id,
          0,
          Some("Unknown authentication scheme"),
          Some("LY860UAC5U"),
          Some(401),
          Some(1390421660154L)))
      }
    }

    "fail with ProfileRetrievalException if an unexpected error occurred" in new WithApplication with Context {
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]
      when(wsResponse.status).thenReturn(500)
      when(wsResponse.json).thenThrow(new RuntimeException(""))
      when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
      when(httpLayer.url(API.format("my.access.token"))).thenReturn(wsRequest)
      mockEmailAndPhoto(httpLayer)
      failed[ProfileRetrievalException](provider.retrieveProfile(oAuthInfo.as[OAuth2Info])) {
        case e => e.getMessage must equalTo(UnspecifiedProfileError.format(provider.id))
      }
    }

    "use the overridden API URL" in new WithApplication with Context {
      val url = "https://custom.api.url?access_token=%s"
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]
      when(oAuthSettings.apiURL).thenReturn(Some(url))
      when(wsResponse.status).thenReturn(200)
      when(wsResponse.json).thenReturn(Helper.loadJson("providers/oauth2/linkedin.success.json"))
      when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
      when(httpLayer.url(url.format("my.access.token"))).thenReturn(wsRequest)
      mockEmailAndPhoto(httpLayer)
      await(provider.retrieveProfile(oAuthInfo.as[OAuth2Info]))

      verify(httpLayer).url(url.format("my.access.token"))
    }

    def mockEmailAndPhoto(httpLayer: MockHTTPLayer) = {
      // Email
      val wsRequestEmail = mock[MockWSRequest]
      val wsResponseEmail = mock[MockWSRequest#Response]
      when(wsResponseEmail.status).thenReturn(200)
      when(wsResponseEmail.json).thenReturn(Helper.loadJson("providers/oauth2/linkedin.email.json"))
      when(wsRequestEmail.get()).thenReturn(Future.successful(wsResponseEmail))
      when(httpLayer.url(EMAIL.format("my.access.token"))).thenReturn(wsRequestEmail)
      // Photo
      val wsRequestPhoto = mock[MockWSRequest]
      val wsResponsePhoto = mock[MockWSRequest#Response]
      when(wsResponsePhoto.status).thenReturn(200)
      when(wsResponsePhoto.json).thenReturn(Helper.loadJson("providers/oauth2/linkedin.photo.json"))
      when(wsRequestPhoto.get()).thenReturn(Future.successful(wsResponsePhoto))
      when(httpLayer.url(PHOTO.format("my.access.token"))).thenReturn(wsRequestPhoto)

    }

    "return the social profile" in new WithApplication with Context {
      // Basic profile
      val wsRequest = mock[MockWSRequest]
      val wsResponse = mock[MockWSRequest#Response]
      when(wsResponse.status).thenReturn(200)
      when(wsResponse.json).thenReturn(Helper.loadJson("providers/oauth2/linkedin.success.json"))
      when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
      when(httpLayer.url(API.format("my.access.token"))).thenReturn(wsRequest)

      mockEmailAndPhoto(httpLayer)

      profile(provider.retrieveProfile(oAuthInfo.as[OAuth2Info])) { p =>
        p must be equalTo CommonSocialProfile(
          loginInfo = LoginInfo(provider.id, "NhZXBl_O6f"),
          firstName = Some("Apollonia"),
          lastName = Some("Vanova"),
          fullName = Some("Apollonia Vanova"),
          email = Some("apollonia.vanova@watchmen.com"),
          avatarURL = Some("https://media.licdn.com/dms/image/C4E03AQFBprjocrF2iA/profile-displayphoto-shrink_100_100/0?e=1576108800&v=beta&t=Tn7mA43w8qmTuzjSdtuYQMi2kI5At9XOp8X--s5hpRU"))
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
     * The OAuth2 settings.
     */
    override lazy val oAuthSettings = spy(OAuth2Settings(
      authorizationURL = Some("https://www.linkedin.com/uas/oauth2/authorization"),
      accessTokenURL = "https://www.linkedin.com/uas/oauth2/accessToken",
      redirectURL = Some("https://www.mohiva.com"),
      clientID = "my.client.id",
      clientSecret = "my.client.secret",
      scope = None))

    /**
     * The OAuth2 info returned by LinkedIn.
     *
     * @see https://developer.linkedin.com/documents/authentication
     */
    override lazy val oAuthInfo = Helper.loadJson("providers/oauth2/linkedin.access.token.json")

    /**
     * The stateful auth info.
     */
    override lazy val stateAuthInfo = StatefulAuthInfo(oAuthInfo.as[OAuth2Info], userStateItem)

    /**
     * The provider to test.
     */
    lazy val provider = new LinkedInProvider(httpLayer, stateProvider, oAuthSettings)
  }
}
