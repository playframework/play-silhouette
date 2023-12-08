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
package play.silhouette.impl.providers

import play.silhouette.api.LoginInfo
import play.silhouette.api.crypto.Base64
import play.silhouette.api.exceptions._
import play.silhouette.api.util._
import play.silhouette.impl.providers.PasswordProvider._
import play.api.test.{ FakeRequest, WithApplication }
import org.mockito.Mockito._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Test case for the [[play.silhouette.impl.providers.BasicAuthProvider]] class.
 */
class BasicAuthProviderSpec extends PasswordProviderSpec {

  "The `authenticate` method" should {
    "throw ConfigurationException if unsupported hasher is stored" in new WithApplication with Context {
      override def running() = {
        val passwordInfo = PasswordInfo("unknown", "hashed(s3cr3t)")
        val loginInfo = LoginInfo(provider.id, credentials.identifier)
        val request = FakeRequest().withHeaders(AUTHORIZATION -> encodeCredentials(credentials))

        when(authInfoRepository.find[PasswordInfo](loginInfo)).thenReturn(Future.successful(Some(passwordInfo)))

        await(provider.authenticate(request)) must throwA[ConfigurationException].like {
          case e => e.getMessage must beEqualTo(HasherIsNotRegistered.format(provider.id, "unknown", "foo, bar"))
        }
      }
    }

    "return None if no auth info could be found for the given credentials" in new WithApplication with Context {
      override def running() = {
        val loginInfo = new LoginInfo(provider.id, credentials.identifier)
        val request = FakeRequest().withHeaders(AUTHORIZATION -> encodeCredentials(credentials))

        when(authInfoRepository.find[PasswordInfo](loginInfo)).thenReturn(Future.successful(None))

        await(provider.authenticate(request)) must beNone
      }
    }

    "return None if password does not match" in new WithApplication with Context {
      override def running() = {
        val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
        val loginInfo = LoginInfo(provider.id, credentials.identifier)
        val request = FakeRequest().withHeaders(AUTHORIZATION -> encodeCredentials(credentials))

        when(fooHasher.matches(passwordInfo, credentials.password)).thenReturn(false)
        when(authInfoRepository.find[PasswordInfo](loginInfo)).thenReturn(Future.successful(Some(passwordInfo)))

        await(provider.authenticate(request)) must beNone
      }
    }

    "return None if provider isn't responsible" in new WithApplication with Context {
      override def running() = {
        await(provider.authenticate(FakeRequest())) must beNone
      }
    }

    "return None for wrong encoded credentials" in new WithApplication with Context {
      override def running() = {
        val request = FakeRequest().withHeaders(AUTHORIZATION -> "wrong")

        await(provider.authenticate(request)) must beNone
      }
    }

    "return login info if passwords does match" in new WithApplication with Context {
      override def running() = {
        val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
        val loginInfo = LoginInfo(provider.id, credentials.identifier)
        val request = FakeRequest().withHeaders(AUTHORIZATION -> encodeCredentials(credentials))

        when(fooHasher.matches(passwordInfo, credentials.password)).thenReturn(true)
        when(authInfoRepository.find[PasswordInfo](loginInfo)).thenReturn(Future.successful(Some(passwordInfo)))

        await(provider.authenticate(request)) must beSome(loginInfo)
      }
    }

    "handle a colon in a password" in new WithApplication with Context {
      override def running() = {
        val credentialsWithColon = Credentials("apollonia.vanova@watchmen.com", "s3c:r3t")
        val passwordInfo = PasswordInfo("foo", "hashed(s3c:r3t)")
        val loginInfo = LoginInfo(provider.id, credentialsWithColon.identifier)
        val request = FakeRequest().withHeaders(AUTHORIZATION -> encodeCredentials(credentialsWithColon))

        when(fooHasher.matches(passwordInfo, credentialsWithColon.password)).thenReturn(true)
        when(authInfoRepository.find[PasswordInfo](loginInfo)).thenReturn(Future.successful(Some(passwordInfo)))

        await(provider.authenticate(request)) must beSome(loginInfo)
      }
    }

    "re-hash password with new hasher if hasher is deprecated" in new WithApplication with Context {
      override def running() = {
        val passwordInfo = PasswordInfo("bar", "hashed(s3cr3t)")
        val loginInfo = LoginInfo(provider.id, credentials.identifier)
        val request = FakeRequest().withHeaders(AUTHORIZATION -> encodeCredentials(credentials))

        when(fooHasher.hash(credentials.password)).thenReturn(passwordInfo)
        when(barHasher.matches(passwordInfo, credentials.password)).thenReturn(true)
        when(authInfoRepository.find[PasswordInfo](loginInfo)).thenReturn(Future.successful(Some(passwordInfo)))
        when(authInfoRepository.update[PasswordInfo](loginInfo, passwordInfo)).thenReturn(Future.successful(passwordInfo))

        await(provider.authenticate(request)) must beSome(loginInfo)
        verify(authInfoRepository).update(loginInfo, passwordInfo)
      }
    }

    "re-hash password with new hasher if password info is deprecated" in new WithApplication with Context {
      override def running() = {
        val passwordInfo = PasswordInfo("foo", "hashed(s3cr3t)")
        val loginInfo = LoginInfo(provider.id, credentials.identifier)
        val request = FakeRequest().withHeaders(AUTHORIZATION -> encodeCredentials(credentials))

        when(fooHasher.isDeprecated(passwordInfo)).thenReturn(Some(true))
        when(fooHasher.hash(credentials.password)).thenReturn(passwordInfo)
        when(fooHasher.matches(passwordInfo, credentials.password)).thenReturn(true)
        when(authInfoRepository.find[PasswordInfo](loginInfo)).thenReturn(Future.successful(Some(passwordInfo)))
        when(authInfoRepository.update[PasswordInfo](loginInfo, passwordInfo)).thenReturn(Future.successful(passwordInfo))

        await(provider.authenticate(request)) must beSome(loginInfo)
        verify(authInfoRepository).update(loginInfo, passwordInfo)
      }
    }

    "return None if Authorization method is not Basic and Base64 decoded header has ':'" in new WithApplication with Context {
      override def running() = {
        val request = FakeRequest().withHeaders(AUTHORIZATION -> Base64.encode("NotBasic foo:bar"))

        await(provider.authenticate(request)) must beNone
      }
    }
  }

  /**
   * The context.
   */
  trait Context extends BaseContext {

    /**
     * The test credentials.
     */
    lazy val credentials = Credentials("apollonia.vanova@watchmen.com", "s3cr3t")

    /**
     * The provider to test.
     */
    lazy val provider = new BasicAuthProvider(authInfoRepository, passwordHasherRegistry)

    /**
     * Creates the credentials to send within the header.
     *
     * @param credentials The credentials to encode.
     * @return The encoded credentials.
     */
    def encodeCredentials(credentials: Credentials) = {
      "Basic " + Base64.encode(s"${credentials.identifier}:${credentials.password}")
    }
  }
}
