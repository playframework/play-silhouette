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
package play.silhouette.impl.providers.custom

import play.silhouette.api.LoginInfo
import play.silhouette.api.util.{ ExtractableRequest, HTTPLayer, MockWSRequest }
import play.silhouette.impl.exceptions.{ ProfileRetrievalException, UnexpectedResponseException }
import play.silhouette.impl.providers.OAuth2Provider._
import play.silhouette.impl.providers.SocialProfileBuilder._
import play.silhouette.impl.providers._
import play.silhouette.impl.providers.oauth2.FacebookProvider._
import play.silhouette.impl.providers.oauth2.{ BaseFacebookProvider, FacebookProfileParser }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{ FakeRequest, WithApplication }
import test.Helper
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Test case for the [[play.silhouette.impl.providers.oauth2.FacebookProvider]] class which uses a custom social profile.
 */
class FacebookProviderSpec extends OAuth2ProviderSpec {

  "The `withSettings` method" should {
    "create a new instance with customized settings" in new WithApplication with Context {
      override def running() = {
        val s: CustomFacebookProvider = provider.withSettings { s =>
          s.copy(accessTokenURL = "new-access-token-url")
        }

        s.settings.accessTokenURL must be equalTo "new-access-token-url"
      }
    }
  }

  "The `authenticate` method" should {
    "fail with UnexpectedResponseException for an unexpected response" in new WithApplication with Context {
      override def running() = {
        val wsRequest = mock(classOf[MockWSRequest])
        val wsResponse = mock(classOf[MockWSRequest#Response])
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
    }

    "fail with UnexpectedResponseException if OAuth2Info can be build because of an unexpected response" in new WithApplication with Context {
      override def running() = {
        val wsRequest = mock(classOf[MockWSRequest])
        val wsResponse = mock(classOf[MockWSRequest#Response])
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
    }

    "return the auth info" in new WithApplication with Context {
      override def running() = {
        val wsRequest = mock(classOf[MockWSRequest])
        val wsResponse = mock(classOf[MockWSRequest#Response])
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
  }

  "The `retrieveProfile` method" should {
    "fail with ProfileRetrievalException if API returns error" in new WithApplication with Context {
      override def running() = {
        val wsRequest = mock(classOf[MockWSRequest])
        val wsResponse = mock(classOf[MockWSRequest#Response])
        when(wsResponse.status).thenReturn(400)
        when(wsResponse.json).thenReturn(Helper.loadJson("providers/custom/facebook.error.json"))
        when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
        when(httpLayer.url(API.format("my.access.token"))).thenReturn(wsRequest)

        failed[ProfileRetrievalException](provider.retrieveProfile(oAuthInfo.as[OAuth2Info])) {
          case e => e.getMessage must equalTo(SpecifiedProfileError.format(
            provider.id,
            "An active access token must be used to query information about the current user.",
            "OAuthException",
            2500))
        }
      }
    }

    "fail with ProfileRetrievalException if an unexpected error occurred" in new WithApplication with Context {
      override def running() = {
        val wsRequest = mock(classOf[MockWSRequest])
        val wsResponse = mock(classOf[MockWSRequest#Response])
        when(wsResponse.status).thenReturn(500)
        when(wsResponse.json).thenThrow(new RuntimeException(""))
        when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
        when(httpLayer.url(API.format("my.access.token"))).thenReturn(wsRequest)

        failed[ProfileRetrievalException](provider.retrieveProfile(oAuthInfo.as[OAuth2Info])) {
          case e => e.getMessage must equalTo(UnspecifiedProfileError.format(provider.id))
        }
      }
    }

    "return the social profile" in new WithApplication with Context {
      override def running() = {
        val wsRequest = mock(classOf[MockWSRequest])
        val wsResponse = mock(classOf[MockWSRequest#Response])
        when(wsResponse.status).thenReturn(200)
        when(wsResponse.json).thenReturn(Helper.loadJson("providers/custom/facebook.success.json"))
        when(wsRequest.get()).thenReturn(Future.successful(wsResponse))
        when(httpLayer.url(API.format("my.access.token"))).thenReturn(wsRequest)

        profile(provider.retrieveProfile(oAuthInfo.as[OAuth2Info])) { p =>
          p must be equalTo CustomSocialProfile(
            loginInfo = LoginInfo(provider.id, "134405962728980"),
            firstName = Some("Apollonia"),
            lastName = Some("Vanova"),
            fullName = Some("Apollonia Vanova"),
            email = Some("apollonia.vanova@watchmen.com"),
            avatarURL = Some("https://fbcdn-sphotos-g-a.akamaihd.net/hphotos-ak-ash2/t1/36245_155530314499277_2350717_n.jpg?lvh=1"),
            gender = Some("male"))
        }
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
      authorizationURL = Some("https://graph.facebook.com/oauth/authorize"),
      accessTokenURL = "https://graph.facebook.com/oauth/access_token",
      redirectURL = Some("https://www.mohiva.com"),
      clientID = "my.client.id",
      clientSecret = "my.client.secret",
      scope = Some("email")))

    /**
     * The OAuth2 info returned by Facebook.
     *
     * @see https://developers.facebook.com/docs/facebook-login/access-tokens
     */
    override lazy val oAuthInfo = Helper.loadJson("providers/oauth2/facebook.access.token.json")

    /**
     * The provider to test.
     */
    lazy val provider: CustomFacebookProvider = new CustomFacebookProvider(httpLayer, stateProvider, oAuthSettings)
  }

  /**
   * A custom social profile for testing purpose.
   */
  case class CustomSocialProfile(
    loginInfo: LoginInfo,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fullName: Option[String] = None,
    email: Option[String] = None,
    avatarURL: Option[String] = None,
    gender: Option[String] = None) extends SocialProfile

  /**
   * A custom Facebook profile parser for testing purpose.
   */
  class CustomFacebookProfileParser extends SocialProfileParser[JsValue, CustomSocialProfile, OAuth2Info] {

    /**
     * The common social profile parser.
     */
    val commonParser = new FacebookProfileParser

    /**
     * Parses the social profile.
     *
     * @param json The content returned from the provider.
     * @param authInfo The auth info to query the provider again for additional data.
     * @return The social profile from given result.
     */
    def parse(json: JsValue, authInfo: OAuth2Info) = commonParser.parse(json, authInfo).map { commonProfile =>
      val gender = (json \ "gender").as[String]
      CustomSocialProfile(
        loginInfo = commonProfile.loginInfo,
        firstName = commonProfile.firstName,
        lastName = commonProfile.lastName,
        fullName = commonProfile.fullName,
        avatarURL = commonProfile.avatarURL,
        email = commonProfile.email,
        gender = Some(gender))
    }
  }

  /**
   * The custom Facebook OAuth2 Provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param stateHandler The state provider implementation.
   * @param settings The provider settings.
   */
  class CustomFacebookProvider(
    protected val httpLayer: HTTPLayer,
    protected val stateHandler: SocialStateHandler,
    val settings: OAuth2Settings)
    extends BaseFacebookProvider {

    /**
     * The type of this class.
     */
    type Self = CustomFacebookProvider

    /**
     * The type of the profile a profile builder is responsible for.
     */
    type Profile = CustomSocialProfile

    /**
     * The profile parser.
     */
    val profileParser = new CustomFacebookProfileParser

    /**
     * Gets a provider initialized with a new settings object.
     *
     * @param f A function which gets the settings passed and returns different settings.
     * @return An instance of the provider initialized with new settings.
     */
    def withSettings(f: (Settings) => Settings) = {
      new CustomFacebookProvider(httpLayer, stateHandler, f(settings))
    }
  }
}
