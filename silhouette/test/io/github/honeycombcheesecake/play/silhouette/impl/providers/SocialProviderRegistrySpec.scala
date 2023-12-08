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

import io.github.honeycombcheesecake.play.silhouette.impl.providers.oauth1.TwitterProvider
import io.github.honeycombcheesecake.play.silhouette.impl.providers.oauth2.{ GoogleProvider, FacebookProvider }
import io.github.honeycombcheesecake.play.silhouette.impl.providers.openid.YahooProvider
import org.specs2.specification.Scope
import play.api.test.PlaySpecification
import org.mockito.Mockito._
import test.Helper.mock

/**
 * Test case for the [[io.github.honeycombcheesecake.play.silhouette.impl.providers.SocialProviderRegistry]] class.
 */
class SocialProviderRegistrySpec extends PlaySpecification {

  "The `get` method" should {
    "return a provider by its type" in new Context {
      registry.get[GoogleProvider] must beSome(providers(1))
    }

    "return None if no provider for the given type exists" in new Context {
      registry.get[YahooProvider] must beNone
    }

    "return a provider by its ID as SocialProvider" in new Context {
      val provider = registry.get[SocialProvider](GoogleProvider.ID)

      provider must beSome[SocialProvider].like {
        case value =>
          value.id must be equalTo providers(1).id
          value must beAnInstanceOf[SocialProvider]
      }
    }

    "return a provider by its ID as OAuth2Provider" in new Context {
      val provider = registry.get[OAuth2Provider](GoogleProvider.ID)

      provider must beSome[OAuth2Provider].like {
        case value =>
          value.id must be equalTo providers(1).id
          value must beAnInstanceOf[OAuth2Provider]
      }
    }

    "return None if no provider for the given ID exists" in new Context {
      registry.get[SocialProvider](YahooProvider.ID) must beNone
    }
  }

  "The `getSeq` method" should {
    "return a list of providers by it's sub type" in new Context {
      val list = registry.getSeq[OAuth2Provider]
      list.head.id must be equalTo providers.head.id
      list(1).id must be equalTo providers(1).id
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * Some social providers.
     */
    val providers = {
      val facebook = mock[FacebookProvider]
      when(facebook.id).thenReturn(FacebookProvider.ID)
      val google = mock[GoogleProvider]
      when(google.id).thenReturn(GoogleProvider.ID)
      val twitter = mock[TwitterProvider]
      when(twitter.id).thenReturn(TwitterProvider.ID)

      Seq(
        facebook,
        google,
        twitter)
    }

    /**
     * The registry to test.
     */
    val registry = SocialProviderRegistry(providers)
  }
}
