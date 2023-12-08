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
package play.silhouette.impl.authenticators

import java.util.regex.Pattern

import play.silhouette.api.Authenticator.Implicits._
import play.silhouette.api.crypto.Base64AuthenticatorEncoder
import play.silhouette.api.exceptions._
import play.silhouette.api.services.AuthenticatorService._
import play.silhouette.api.util.{ Clock, FingerprintGenerator }
import play.silhouette.api.{ Authenticator, LoginInfo }
import play.silhouette.impl.authenticators.SessionAuthenticator._
import play.silhouette.impl.authenticators.SessionAuthenticatorService._
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import test.Helper.{ mockSmart, mock }

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Test case for the [[play.silhouette.impl.authenticators.SessionAuthenticator]].
 */
class SessionAuthenticatorSpec extends PlaySpecification {

  "The `isValid` method of the authenticator" should {
    "return false if the authenticator is expired" in new Context {
      authenticator.copy(expirationDateTime = ZonedDateTime.now - 1.hour).isValid must beFalse
    }

    "return false if the authenticator is timed out" in new Context {
      authenticator.copy(
        lastUsedDateTime = ZonedDateTime.now - (settings.authenticatorIdleTimeout.get + 1.second)).isValid must beFalse
    }

    "return true if the authenticator is valid" in new Context {
      authenticator.copy(
        lastUsedDateTime = ZonedDateTime.now - (settings.authenticatorIdleTimeout.get - 10.seconds),
        expirationDateTime = ZonedDateTime.now + 5.seconds).isValid must beTrue
    }
  }

  "The `unserialize` method of the authenticator" should {
    "throw an AuthenticatorException if the given value can't be parsed as Json" in new WithApplication with Context {
      override def running() = {
        val value = "invalid"
        val msg = Pattern.quote(JsonParseError.format(ID, value))

        unserialize(authenticatorEncoder.encode(value), authenticatorEncoder) must beFailedTry.withThrowable[AuthenticatorException](msg)
      }
    }

    "throw an AuthenticatorException if the given value is in the wrong Json format" in new WithApplication with Context {
      override def running() = {
        val value = "{}"
        val msg = "^" + Pattern.quote(InvalidJsonFormat.format(ID, "")) + ".*"

        unserialize(authenticatorEncoder.encode(value), authenticatorEncoder) must beFailedTry.withThrowable[AuthenticatorException](msg)
      }
    }
  }

  "The `serialize/unserialize` method of the authenticator" should {
    "serialize/unserialize an authenticator" in new WithApplication with Context {
      override def running() = {
        val value = serialize(authenticator, authenticatorEncoder)

        unserialize(value, authenticatorEncoder) must beSuccessfulTry.withValue(authenticator)
      }
    }
  }

  "The `create` method of the service" should {
    "return a fingerprinted authenticator" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      when(clock.now).thenReturn(ZonedDateTime.now)
      when(fingerprintGenerator.generate(any)).thenReturn("test")
      when(settings.useFingerprinting).thenReturn(true)

      await(service.create(loginInfo)).fingerprint must beSome("test")
    }

    "return a non fingerprinted authenticator" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      when(clock.now).thenReturn(ZonedDateTime.now)
      when(settings.useFingerprinting).thenReturn(false)

      await(service.create(loginInfo)).fingerprint must beNone
    }

    "return an authenticator with the current date as lastUsedDateTime" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val now = ZonedDateTime.now

      when(clock.now).thenReturn(now)

      await(service.create(loginInfo)).lastUsedDateTime must be equalTo now
    }

    "return an authenticator which expires in 12 hours(default value)" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val now = ZonedDateTime.now

      when(clock.now).thenReturn(now)

      await(service.create(loginInfo)).expirationDateTime must be equalTo now + 12.hours
    }

    "return an authenticator which expires in 6 hours" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val sixHours = 6 hours
      val now = ZonedDateTime.now

      when(clock.now).thenReturn(now)
      when(settings.authenticatorExpiry).thenReturn(sixHours)

      await(service.create(loginInfo)).expirationDateTime must be equalTo now + sixHours
    }

    "throws an AuthenticatorCreationException exception if an error occurred during creation" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      when(clock.now).thenThrow(new RuntimeException("Could not get date"))

      await(service.create(loginInfo)) must throwA[AuthenticatorCreationException].like {
        case e =>
          e.getMessage must startWith(CreateError.format(ID, ""))
      }
    }
  }

  "The `retrieve` method of the service" should {
    "return None if no authenticator exists in session" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      await(service.retrieve) must beNone
    }

    "return None if session contains invalid json" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> authenticatorEncoder.encode("{"))

        when(settings.useFingerprinting).thenReturn(false)

        await(service.retrieve) must beNone
      }
    }

    "return None if session contains valid json but invalid authenticator" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> authenticatorEncoder.encode("{ \"test\": \"test\" }"))

        when(settings.useFingerprinting).thenReturn(false)

        await(service.retrieve) must beNone
      }
    }

    "return None if authenticator fingerprint doesn't match current fingerprint" in new WithApplication with Context {
      override def running() = {
        when(fingerprintGenerator.generate(any)).thenReturn("false")
        when(settings.useFingerprinting).thenReturn(true)
        when(authenticator.fingerprint).thenReturn(Some("test"))

        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> authenticatorEncoder.encode(Json.toJson(authenticator).toString()))

        await(service.retrieve) must beNone
      }
    }

    "return authenticator if authenticator fingerprint matches current fingerprint" in new WithApplication with Context {
      override def running() = {
        when(fingerprintGenerator.generate(any)).thenReturn("test")
        when(settings.useFingerprinting).thenReturn(true)
        when(authenticator.fingerprint).thenReturn(Some("test"))

        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> authenticatorEncoder.encode(Json.toJson(authenticator).toString()))

        await(service.retrieve) must beSome(authenticator)
      }
    }

    "return authenticator if fingerprinting is disabled" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> authenticatorEncoder.encode(Json.toJson(authenticator).toString()))

        when(settings.useFingerprinting).thenReturn(false)

        await(service.retrieve) must beSome(authenticator)
      }
    }

    "decode an authenticator" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
          .withSession(settings.sessionKey -> authenticatorEncoder.encode(Json.toJson(authenticator).toString()))

        when(settings.useFingerprinting).thenReturn(false)

        await(service.retrieve) must beSome(authenticator)
      }
    }

    "throws an AuthenticatorRetrievalException exception if an error occurred during retrieval" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> authenticatorEncoder.encode(Json.toJson(authenticator).toString()))

        when(fingerprintGenerator.generate(any)).thenThrow(new RuntimeException("Could not generate fingerprint"))
        when(settings.useFingerprinting).thenReturn(true)

        await(service.retrieve) must throwA[AuthenticatorRetrievalException].like {
          case e =>
            e.getMessage must startWith(RetrieveError.format(ID, ""))
        }
      }
    }
  }

  "The `init` method of the service" should {
    "return a session with an encoded authenticator" in new WithApplication with AppContext {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
        val data = authenticatorEncoder.encode(Json.toJson(authenticator).toString())
        val session = await(service.init(authenticator))

        session must be equalTo sessionCookieBaker.deserialize(Map(settings.sessionKey -> data))
      }
    }

    "override existing authenticator from request" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> "existing")
        val session = await(service.init(authenticator))

        unserialize(session.get(settings.sessionKey).get, authenticatorEncoder).get must be equalTo authenticator
      }
    }

    "keep non authenticator related session data" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession("test" -> "test")
        val data = authenticatorEncoder.encode(Json.toJson(authenticator).toString())
        val session = await(service.init(authenticator))

        session.get(settings.sessionKey) should beSome(data)
        session.get("test") should beSome("test")
      }
    }
  }

  "The result `embed` method of the service" should {
    "return the response with the session" in new WithApplication with AppContext {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
        val data = authenticatorEncoder.encode(Json.toJson(authenticator).toString())
        val result = service.embed(sessionCookieBaker.deserialize(Map(settings.sessionKey -> data)), Results.Ok)

        session(result).get(settings.sessionKey) should beSome(data)
      }
    }

    "override existing authenticator from request" in new WithApplication with AppContext {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> "existing")
        val data = authenticatorEncoder.encode(Json.toJson(authenticator).toString())
        val result = service.embed(sessionCookieBaker.deserialize(Map(settings.sessionKey -> data)), Results.Ok)

        session(result).get(settings.sessionKey) should beSome(data)
      }
    }

    "keep non authenticator related session data" in new WithApplication with AppContext {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession("request-other" -> "keep")
        val data = authenticatorEncoder.encode(Json.toJson(authenticator).toString())
        val result = service.embed(sessionCookieBaker.deserialize(Map(settings.sessionKey -> data)), Results.Ok.addingToSession(
          "result-other" -> "keep"))

        session(result).get(settings.sessionKey) should beSome(data)
        session(result).get("request-other") should beSome("keep")
        session(result).get("result-other") should beSome("keep")
      }
    }
  }

  "The request `embed` method of the service" should {
    "return the request with the session" in new WithApplication with AppContext {
      override def running() = {
        val data = authenticatorEncoder.encode(Json.toJson(authenticator).toString())
        val session = sessionCookieBaker.deserialize(Map(settings.sessionKey -> data))
        val request = service.embed(session, FakeRequest())

        request.session.get(settings.sessionKey) should beSome(data)
      }
    }

    "override an existing session" in new WithApplication with AppContext {
      override def running() = {
        val data = authenticatorEncoder.encode(Json.toJson(authenticator).toString())
        val session = sessionCookieBaker.deserialize(Map(settings.sessionKey -> data))
        val request = service.embed(session, FakeRequest().withSession(settings.sessionKey -> "test"))

        request.session.get(settings.sessionKey) should beSome(data)
      }
    }

    "should not remove an existing session key" in new WithApplication with AppContext {
      override def running() = {
        val session = sessionCookieBaker.deserialize(Map(settings.sessionKey -> "test"))
        val request = service.embed(session, FakeRequest().withSession("existing" -> "test"))

        request.session.get("existing") should beSome("test")
        request.session.get(settings.sessionKey) should beSome("test")
      }
    }

    "keep other request parts" in new WithApplication with AppContext {
      override def running() = {
        val session = sessionCookieBaker.deserialize(Map(settings.sessionKey -> "test"))
        val request = service.embed(session, FakeRequest().withCookies(Cookie("test", "test")))

        request.session.get(settings.sessionKey) should beSome("test")
        request.cookies.get("test") should beSome[Cookie].which { c =>
          c.name must be equalTo "test"
          c.value must be equalTo "test"
        }
      }
    }
  }

  "The `touch` method of the service" should {
    "update the last used date if idle timeout is defined" in new WithApplication with Context {
      override def running() = {
        when(settings.authenticatorIdleTimeout).thenReturn(Some(1 second))
        when(clock.now).thenReturn(ZonedDateTime.now)

        service.touch(authenticator) must beLeft[SessionAuthenticator].like {
          case a =>
            a.lastUsedDateTime must be equalTo clock.now
        }
      }
    }

    "do not update the last used date if idle timeout is not defined" in new WithApplication with Context {
      override def running() = {
        when(settings.authenticatorIdleTimeout).thenReturn(None)
        when(clock.now).thenReturn(ZonedDateTime.now)

        service.touch(authenticator) must beRight[SessionAuthenticator].like {
          case a =>
            a.lastUsedDateTime must be equalTo authenticator.lastUsedDateTime
        }
      }
    }
  }

  "The `update` method of the service" should {
    "update the session" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
        val data = authenticatorEncoder.encode(Json.toJson(authenticator).toString())
        val result = service.update(authenticator, Results.Ok)

        status(result) must be equalTo OK
        session(result).get(settings.sessionKey) should beSome(data)
      }
    }

    "override existing authenticator from request" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> "existing")
        val result = service.update(authenticator, Results.Ok)

        unserialize(session(result).get(settings.sessionKey).get, authenticatorEncoder).get must be equalTo authenticator
      }
    }

    "non authenticator related session data" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession("request-other" -> "keep")
        val result = service.update(authenticator, Results.Ok.addingToSession(
          "result-other" -> "keep"))

        unserialize(session(result).get(settings.sessionKey).get, authenticatorEncoder).get must be equalTo authenticator
        session(result).get("request-other") should beSome("keep")
        session(result).get("result-other") should beSome("keep")
      }
    }

    "throws an AuthenticatorUpdateException exception if an error occurred during update" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = spy(FakeRequest())

      when(request.session).thenThrow(new RuntimeException("Cannot get session"))

      await(service.update(authenticator, Results.Ok)) must throwA[AuthenticatorUpdateException].like {
        case e =>
          e.getMessage must startWith(UpdateError.format(ID, ""))
      }
    }
  }

  "The `renew` method of the service" should {
    "renew the session" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
        val now = ZonedDateTime.now
        val data = authenticatorEncoder.encode(Json.toJson(authenticator.copy(
          lastUsedDateTime = now,
          expirationDateTime = now + settings.authenticatorExpiry)).toString())

        when(settings.useFingerprinting).thenReturn(false)
        when(clock.now).thenReturn(now)

        val result = service.renew(authenticator, Results.Ok)

        session(result).get(settings.sessionKey) should beSome(data)
      }
    }

    "override existing authenticator from request" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(settings.sessionKey -> "existing")
        val now = ZonedDateTime.now

        when(settings.useFingerprinting).thenReturn(false)
        when(clock.now).thenReturn(now)

        val result = service.renew(authenticator, Results.Ok)

        unserialize(session(result).get(settings.sessionKey).get, authenticatorEncoder).get must be equalTo authenticator.copy(
          lastUsedDateTime = now,
          expirationDateTime = now + settings.authenticatorExpiry)
      }
    }

    "non authenticator related session data" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession("request-other" -> "keep")
        val now = ZonedDateTime.now

        when(settings.useFingerprinting).thenReturn(false)
        when(clock.now).thenReturn(now)

        val result = service.renew(authenticator, Results.Ok.addingToSession(
          "result-other" -> "keep"))

        unserialize(session(result).get(settings.sessionKey).get, authenticatorEncoder).get must be equalTo authenticator.copy(
          lastUsedDateTime = now,
          expirationDateTime = now + settings.authenticatorExpiry)
        session(result).get("request-other") should beSome("keep")
        session(result).get("result-other") should beSome("keep")
      }
    }

    "throws an AuthenticatorRenewalException exception if an error occurred during renewal" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = spy(FakeRequest())
      val now = ZonedDateTime.now
      val okResult = (_: Authenticator) => Future.successful(Results.Ok)

      when(request.session).thenThrow(new RuntimeException("Cannot get session"))
      when(settings.useFingerprinting).thenReturn(false)
      when(clock.now).thenReturn(now)

      await(service.renew(authenticator, Results.Ok)) must throwA[AuthenticatorRenewalException].like {
        case e =>
          e.getMessage must startWith(RenewError.format(ID, ""))
      }
    }
  }

  "The `discard` method of the service" should {
    "discard the authenticator from session" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
        val result = service.discard(authenticator, Results.Ok.withSession(
          settings.sessionKey -> "test"))

        session(result).get(settings.sessionKey) should beNone
      }
    }

    "non authenticator related session data" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession("request-other" -> "keep", settings.sessionKey -> "test")
        val result = service.discard(authenticator, Results.Ok.addingToSession(
          "result-other" -> "keep"))

        session(result).get(settings.sessionKey) should beNone
        session(result).get("request-other") should beSome("keep")
        session(result).get("result-other") should beSome("keep")
      }
    }

    "throws an AuthenticatorDiscardingException exception if an error occurred during discarding" in new WithApplication with Context {
      override def running() = {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = spy(FakeRequest()).withSession(settings.sessionKey -> "test")
        val result = mock[Result]

        when(result.removingFromSession(any)(any)).thenThrow(new RuntimeException("Cannot get session"))

        await(service.discard(authenticator, result)) must throwA[AuthenticatorDiscardingException].like {
          case e =>
            e.getMessage must startWith(DiscardError.format(ID, ""))
        }
      }
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The ID generator implementation.
     */
    lazy val fingerprintGenerator = mockSmart[FingerprintGenerator]

    /**
     * The authenticator encoder implementation.
     */
    lazy val authenticatorEncoder = new Base64AuthenticatorEncoder

    /**
     * The clock implementation.
     */
    lazy val clock = mockSmart[Clock]

    /**
     * The settings.
     */
    lazy val settings = spy(SessionAuthenticatorSettings(
      authenticatorIdleTimeout = Some(30 minutes),
      authenticatorExpiry = 12 hours))

    /**
     * The cache service instance to test.
     */
    lazy val service = new SessionAuthenticatorService(
      settings,
      fingerprintGenerator,
      authenticatorEncoder,
      new DefaultSessionCookieBaker(),
      clock)

    /**
     * The login info.
     */
    lazy val loginInfo = LoginInfo("test", "1")

    /**
     * An authenticator.
     */
    lazy val authenticator = spy(new SessionAuthenticator(
      loginInfo = LoginInfo("test", "1"),
      lastUsedDateTime = ZonedDateTime.now,
      expirationDateTime = ZonedDateTime.now + settings.authenticatorExpiry,
      idleTimeout = settings.authenticatorIdleTimeout,
      fingerprint = None))
  }

  /**
   * The application context.
   */
  trait AppContext extends Context {
    self: WithApplication =>

    /**
     * The session cookie baker instance.
     */
    lazy val sessionCookieBaker = app.injector.instanceOf[SessionCookieBaker]
  }
}
