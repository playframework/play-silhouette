package play.silhouette.impl.providers.openid.service

import play.silhouette.impl.providers.OpenIDSettings
import play.silhouette.impl.providers.openid.services.PlayOpenIDService
import org.specs2.specification.Scope
import org.mockito.Mockito.mock
import play.api.libs.openid.OpenIdClient
import play.api.test.{ PlaySpecification, WithApplication }

class PlayOpenIDServiceSpec extends PlaySpecification {

  "The `withSettings` method" should {
    "create a new instance with customized settings" in new WithApplication with Context {
      override def running() = {
        val s = service.withSettings { s =>
          s.copy("new-provider-url")
        }

        s.settings.providerURL must be equalTo "new-provider-url"
      }
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {
    /**
     * The OpenID settings.
     */
    lazy val openIDSettings = OpenIDSettings(
      providerURL = "https://me.yahoo.com/",
      callbackURL = "http://localhost:9000/authenticate/yahoo",
      axRequired = Map(
        "fullname" -> "http://axschema.org/namePerson",
        "email" -> "http://axschema.org/contact/email",
        "image" -> "http://axschema.org/media/image/default"),
      realm = Some("http://localhost:9000"))

    lazy val service = new PlayOpenIDService(mock(classOf[OpenIdClient]), openIDSettings)
  }

}
