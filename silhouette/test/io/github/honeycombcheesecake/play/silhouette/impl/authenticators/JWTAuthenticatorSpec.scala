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
package io.github.honeycombcheesecake.play.silhouette.impl.authenticators

import java.util.regex.Pattern
import io.github.honeycombcheesecake.play.silhouette.api.Authenticator.Implicits._
import io.github.honeycombcheesecake.play.silhouette.api.LoginInfo
import io.github.honeycombcheesecake.play.silhouette.api.crypto.{ Base64, Base64AuthenticatorEncoder }
import io.github.honeycombcheesecake.play.silhouette.api.exceptions._
import io.github.honeycombcheesecake.play.silhouette.api.repositories.AuthenticatorRepository
import io.github.honeycombcheesecake.play.silhouette.api.services.AuthenticatorService._
import io.github.honeycombcheesecake.play.silhouette.api.util.{ Clock, IDGenerator, RequestPart }
import io.github.honeycombcheesecake.play.silhouette.impl.authenticators.JWTAuthenticator._
import io.github.honeycombcheesecake.play.silhouette.impl.authenticators.JWTAuthenticatorService._
import org.specs2.control.NoLanguageFeatures
import org.specs2.matcher.JsonMatchers
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.libs.json.{ JsNull, JsObject, Json }
import play.api.mvc.Results
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }

import java.time.temporal.ChronoField
import java.time.{ ZoneId, ZonedDateTime }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Test case for the [[io.github.honeycombcheesecake.play.silhouette.impl.authenticators.JWTAuthenticator]].
 */
class JWTAuthenticatorSpec extends PlaySpecification with Mockito with JsonMatchers with NoLanguageFeatures {

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

  "The `serialize` method of the authenticator" should {
    "return a JWT with an expiration time" in new WithApplication with Context {
      val jwt = serialize(authenticator, authenticatorEncoder, settings)
      val json = Base64.decode(jwt.split('.').apply(1))

      json must /("exp" -> authenticator.expirationDateTime.toEpochSecond.toInt)
    }

    "return a JWT with an encoded subject" in new WithApplication with Context {
      val jwt = serialize(authenticator, authenticatorEncoder, settings)
      val json = Json.parse(Base64.decode(jwt.split('.').apply(1)))
      val sub = Json.parse(authenticatorEncoder.decode((json \ "sub").as[String])).as[LoginInfo]

      sub must be equalTo authenticator.loginInfo
    }

    "return a JWT with an issuer" in new WithApplication with Context {
      val jwt = serialize(authenticator, authenticatorEncoder, settings)
      val json = Base64.decode(jwt.split('.').apply(1))

      json must /("iss" -> settings.issuerClaim)
    }

    "return a JWT with an issued-at time" in new WithApplication with Context {
      val jwt = serialize(authenticator, authenticatorEncoder, settings)
      val json = Base64.decode(jwt.split('.').apply(1))

      json must /("iat" -> authenticator.lastUsedDateTime.toEpochSecond.toInt)
    }

    "throw an AuthenticatorException if a reserved claim will be overridden" in new WithApplication with Context {
      val claims = Json.obj(
        "jti" -> "reserved")

      serialize(authenticator.copy(customClaims = Some(claims)), authenticatorEncoder, settings) must throwA[AuthenticatorException].like {
        case e => e.getMessage must startWith(OverrideReservedClaim.format(ID, "jti", ""))
      }
    }

    "throw an AuthenticatorException if an unexpected value was found in the arbitrary claims" in new WithApplication with Context {
      val claims = Json.obj(
        "null" -> JsNull)

      serialize(authenticator.copy(customClaims = Some(claims)), authenticatorEncoder, settings) must throwA[AuthenticatorException].like {
        case e => e.getMessage must startWith(UnexpectedJsonValue.format(ID, ""))
      }
    }

    "return a JWT with arbitrary claims" in new WithApplication with Context {
      val jwt = serialize(authenticator.copy(customClaims = Some(customClaims)), authenticatorEncoder, settings)
      val json = Base64.decode(jwt.split('.').apply(1))

      json must /("boolean" -> true)
      json must /("string" -> "string")
      json must /("number" -> 1234567890)
      json must /("array") /# 0 / 1
      json must /("array") /# 1 / 2
      json must /("object") / "array" /# 0 / "string1"
      json must /("object") / "array" /# 1 / "string2"
      json must /("object") / "object" / "array" /# 0 / "string"
      json must /("object") / "object" / "array" /# 1 / false
      json must /("object") / "object" / "array" /# 2 / ("number" -> 1)
    }
  }

  "The `unserialize` method of the authenticator" should {
    "throw an AuthenticatorException if the given token can't be parsed" in new WithApplication with Context {
      val jwt = "invalid"
      val msg = Pattern.quote(InvalidJWTToken.format(ID, jwt))

      unserialize(jwt, authenticatorEncoder, settings) must beFailedTry.withThrowable[AuthenticatorException](msg)
    }

    "throw an AuthenticatorException if the given token couldn't be verified" in new WithApplication with Context {
      val jwt = serialize(authenticator, authenticatorEncoder, settings) + "-wrong-sig"
      val msg = Pattern.quote(InvalidJWTToken.format(ID, jwt))

      unserialize(jwt, authenticatorEncoder, settings) must beFailedTry.withThrowable[AuthenticatorException](msg)
    }

    "unserialize a JWT" in new WithApplication with Context {
      val jwt = serialize(authenticator, authenticatorEncoder, settings)

      unserialize(jwt, authenticatorEncoder, settings) must beSuccessfulTry.withValue(authenticator.copy(
        expirationDateTime = authenticator.expirationDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0),
        lastUsedDateTime = authenticator.lastUsedDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0)))
    }

    "unserialize a JWT with a custom clock" in new WithApplication with Context {

      val lastUsedDateTime: ZonedDateTime = ZonedDateTime
        .of(2015, 2, 25, 19, 0, 0, 0, ZoneId.systemDefault())
        .`with`(ChronoField.MILLI_OF_SECOND, 0)

      val authenticatorCustomClock: JWTAuthenticator = authenticator
        .copy(
          expirationDateTime = lastUsedDateTime + settings.authenticatorExpiry,
          lastUsedDateTime = lastUsedDateTime)

      val jwt: String = serialize(authenticatorCustomClock, authenticatorEncoder, settings)

      clock.now returns lastUsedDateTime
      implicit val customClock: Option[Clock] = Some(clock)

      unserialize(jwt, authenticatorEncoder, settings) must beSuccessfulTry.withValue(authenticatorCustomClock.copy(
        expirationDateTime = authenticatorCustomClock.expirationDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0),
        lastUsedDateTime = authenticatorCustomClock.lastUsedDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0)))
    }

    "unserialize a JWT with arbitrary claims" in new WithApplication with Context {
      val jwt = serialize(authenticator.copy(customClaims = Some(customClaims)), authenticatorEncoder, settings)

      unserialize(jwt, authenticatorEncoder, settings) must beSuccessfulTry.like {
        case a =>
          a.customClaims must beSome(customClaims)
      }
    }
  }

  "The `serialize/unserialize` method of the authenticator" should {
    "serialize/unserialize an authenticator" in new WithApplication with Context {
      val jwt = serialize(authenticator, authenticatorEncoder, settings)

      unserialize(jwt, authenticatorEncoder, settings) must beSuccessfulTry.withValue(authenticator)
    }
  }

  "The `create` method of the service" should {
    "return an authenticator with the generated ID" in new Context {
      implicit val request = FakeRequest()
      val id = "test-id"

      idGenerator.generate returns Future.successful(id)
      clock.now returns ZonedDateTime.now

      await(service(None).create(loginInfo)).id must be equalTo id
    }

    "return an authenticator with the current date as lastUsedDateTime" in new Context {
      implicit val request = FakeRequest()
      val now = ZonedDateTime.now

      idGenerator.generate returns Future.successful("test-id")
      clock.now returns now

      await(service(None).create(loginInfo)).lastUsedDateTime must be equalTo now
    }

    "return an authenticator which expires in 12 hours(default value)" in new Context {
      implicit val request = FakeRequest()
      val now = ZonedDateTime.now

      idGenerator.generate returns Future.successful("test-id")
      clock.now returns now

      await(service(None).create(loginInfo)).expirationDateTime must be equalTo now + 12.hours
    }

    "return an authenticator which expires in 6 hours" in new Context {
      implicit val request = FakeRequest()
      val sixHours = 6 hours
      val now = ZonedDateTime.now

      settings.authenticatorExpiry returns sixHours
      idGenerator.generate returns Future.successful("test-id")
      clock.now returns now

      await(service(None).create(loginInfo)).expirationDateTime must be equalTo now + sixHours
    }

    "throws an AuthenticatorCreationException exception if an error occurred during creation" in new Context {
      implicit val request = FakeRequest()

      idGenerator.generate returns Future.failed(new Exception("Could not generate ID"))

      await(service(None).create(loginInfo)) must throwA[AuthenticatorCreationException].like {
        case e =>
          e.getMessage must startWith(CreateError.format(ID, ""))
      }
    }
  }

  "The `retrieve` method of the service" should {
    "return None if no authenticator header exists" in new Context {
      implicit val request = FakeRequest()

      await(service(None).retrieve) must beNone
    }

    "return None if DAO is enabled and no authenticator is stored for the token located in the header" in new Context {
      implicit val request = FakeRequest().withHeaders(settings.fieldName -> "not-stored")

      repository.find(authenticator.id) returns Future.successful(None)

      await(service(Some(repository)).retrieve) must beNone
    }

    "return authenticator if DAO is enabled and an authenticator is stored for the token located in the the header" in new WithApplication with Context {
      implicit val request = FakeRequest().withHeaders(settings.fieldName -> serialize(authenticator, authenticatorEncoder, settings))
      clock.now returns ZonedDateTime.now

      repository.find(authenticator.id) returns Future.successful(Some(authenticator.copy(
        expirationDateTime = authenticator.expirationDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0),
        lastUsedDateTime = authenticator.lastUsedDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0))))

      await(service(Some(repository)).retrieve) must beSome(authenticator.copy(
        expirationDateTime = authenticator.expirationDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0),
        lastUsedDateTime = authenticator.lastUsedDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0)))
    }

    "return authenticator if DAO is enabled and an authenticator is stored for the token located in the the query string" in new WithApplication with Context {
      implicit val request = FakeRequest("GET", s"?${settings.fieldName}=${serialize(authenticator, authenticatorEncoder, settings)}")
      clock.now returns ZonedDateTime.now

      settings.requestParts returns Some(Seq(RequestPart.QueryString))
      repository.find(authenticator.id) returns Future.successful(Some(authenticator.copy(
        expirationDateTime = authenticator.expirationDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0),
        lastUsedDateTime = authenticator.lastUsedDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0))))

      await(service(Some(repository)).retrieve) must beSome(authenticator.copy(
        expirationDateTime = authenticator.expirationDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0),
        lastUsedDateTime = authenticator.lastUsedDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0)))
    }

    "return authenticator if DAO is disabled and authenticator was found in the header" in new WithApplication with Context {
      implicit val request = FakeRequest().withHeaders(settings.fieldName -> serialize(authenticator, authenticatorEncoder, settings))
      clock.now returns ZonedDateTime.now

      await(service(None).retrieve) must beSome(authenticator.copy(
        expirationDateTime = authenticator.expirationDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0),
        lastUsedDateTime = authenticator.lastUsedDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0)))
      there was no(repository).find(any())
    }

    "return authenticator if DAO is disabled and authenticator was found in the query string" in new WithApplication with Context {
      implicit val request = FakeRequest("GET", s"?${settings.fieldName}=${serialize(authenticator, authenticatorEncoder, settings)}")
      clock.now returns ZonedDateTime.now

      settings.requestParts returns Some(Seq(RequestPart.QueryString))
      await(service(None).retrieve) must beSome(authenticator.copy(
        expirationDateTime = authenticator.expirationDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0),
        lastUsedDateTime = authenticator.lastUsedDateTime.`with`(ChronoField.MILLI_OF_SECOND, 0)))
      there was no(repository).find(any())
    }

    "throws an AuthenticatorRetrievalException exception if an error occurred during retrieval" in new WithApplication with Context {
      implicit val request = FakeRequest().withHeaders(settings.fieldName -> serialize(authenticator, authenticatorEncoder, settings))
      clock.now returns ZonedDateTime.now

      repository.find(authenticator.id) returns Future.failed(new RuntimeException("Cannot find authenticator"))

      await(service(Some(repository)).retrieve) must throwA[AuthenticatorRetrievalException].like {
        case e =>
          e.getMessage must startWith(RetrieveError.format(ID, ""))
      }
    }
  }

  "The `init` method of the service" should {
    "return the token if DAO is enabled and authenticator could be saved in backing store" in new WithApplication with Context {
      repository.add(any()) answers { p: Any => Future.successful(p.asInstanceOf[JWTAuthenticator]) }
      implicit val request = FakeRequest()

      val token = await(service(Some(repository)).init(authenticator))

      unserialize(token, authenticatorEncoder, settings).get must be equalTo authenticator
      there was one(repository).add(any())
    }

    "return the token if DAO is disabled" in new WithApplication with Context {
      implicit val request = FakeRequest()

      val token = await(service(None).init(authenticator))

      unserialize(token, authenticatorEncoder, settings).get must be equalTo authenticator
      there was no(repository).add(any())
    }

    "throws an AuthenticatorInitializationException exception if an error occurred during initialization" in new Context {
      repository.add(any()) returns Future.failed(new Exception("Cannot store authenticator"))

      implicit val request = FakeRequest()
      val okResult = Future.successful(Results.Ok)

      await(service(Some(repository)).init(authenticator)) must throwA[AuthenticatorInitializationException].like {
        case e =>
          e.getMessage must startWith(InitError.format(ID, ""))
      }
    }
  }

  "The result `embed` method of the service" should {
    "return the response with a header" in new WithApplication with Context {
      implicit val request = FakeRequest()
      val token = serialize(authenticator, authenticatorEncoder, settings)

      val result = service(Some(repository)).embed(token, Results.Ok)

      header(settings.fieldName, result) should beSome(token)
    }
  }

  "The request `embed` method of the service" should {
    "return the request with a header " in new WithApplication with Context {
      val token = serialize(authenticator, authenticatorEncoder, settings)
      val request = service(Some(repository)).embed(token, FakeRequest())

      unserialize(request.headers.get(settings.fieldName).get, authenticatorEncoder, settings).get must be equalTo authenticator
    }

    "override an existing token" in new WithApplication with Context {
      val token = serialize(authenticator, authenticatorEncoder, settings)
      val request = service(Some(repository)).embed(token, FakeRequest().withHeaders(settings.fieldName -> "test"))

      unserialize(request.headers.get(settings.fieldName).get, authenticatorEncoder, settings).get must be equalTo authenticator
    }

    "keep non authenticator related headers" in new WithApplication with Context {
      val token = serialize(authenticator, authenticatorEncoder, settings)
      val request = service(Some(repository)).embed(token, FakeRequest().withHeaders("test" -> "test"))

      unserialize(request.headers.get(settings.fieldName).get, authenticatorEncoder, settings).get must be equalTo authenticator
      request.headers.get("test") should beSome("test")
    }

    "keep other request parts" in new WithApplication with Context {
      val token = serialize(authenticator, authenticatorEncoder, settings)
      val request = service(Some(repository)).embed(token, FakeRequest().withSession("test" -> "test"))

      unserialize(request.headers.get(settings.fieldName).get, authenticatorEncoder, settings).get must be equalTo authenticator
      request.session.get("test") should beSome("test")
    }
  }

  "The `touch` method of the service" should {
    "update the last used date if idle timeout is defined" in new WithApplication with Context {
      settings.authenticatorIdleTimeout returns Some(1 second)
      clock.now returns ZonedDateTime.now

      service(None).touch(authenticator) must beLeft[JWTAuthenticator].like {
        case a =>
          a.lastUsedDateTime must be equalTo clock.now
      }
    }

    "do not update the last used date if idle timeout is not defined" in new WithApplication with Context {
      settings.authenticatorIdleTimeout returns None
      clock.now returns ZonedDateTime.now

      service(None).touch(authenticator) must beRight[JWTAuthenticator].like {
        case a =>
          a.lastUsedDateTime must be equalTo authenticator.lastUsedDateTime
      }
    }
  }

  "The `update` method of the service" should {
    "update the authenticator in backing store" in new WithApplication with Context {
      repository.update(any()) answers { _: Any => Future.successful(authenticator) }

      implicit val request = FakeRequest()

      await(service(Some(repository)).update(authenticator, Results.Ok))

      there was one(repository).update(authenticator)
    }

    "return the result if the authenticator could be stored in backing store" in new WithApplication with Context {
      repository.update(any()) answers { p: Any => Future.successful(p.asInstanceOf[JWTAuthenticator]) }

      implicit val request = FakeRequest()
      val result = service(Some(repository)).update(authenticator, Results.Ok)

      status(result) must be equalTo OK
      unserialize(header(settings.fieldName, result).get, authenticatorEncoder, settings).get must be equalTo authenticator
      there was one(repository).update(authenticator)
    }

    "return the result if backing store is disabled" in new WithApplication with Context {
      repository.update(any()) answers { p: Any => Future.successful(p.asInstanceOf[JWTAuthenticator]) }

      implicit val request = FakeRequest()
      val result = service(None).update(authenticator, Results.Ok)

      status(result) must be equalTo OK
      unserialize(header(settings.fieldName, result).get, authenticatorEncoder, settings).get must be equalTo authenticator
      there was no(repository).update(any())
    }

    "throws an AuthenticatorUpdateException exception if an error occurred during update" in new WithApplication with Context {
      repository.update(any()) returns Future.failed(new Exception("Cannot store authenticator"))

      implicit val request = FakeRequest()

      await(service(Some(repository)).update(authenticator, Results.Ok)) must throwA[AuthenticatorUpdateException].like {
        case e =>
          e.getMessage must startWith(UpdateError.format(ID, ""))
      }
    }
  }

  "The `renew` method of the service" should {
    "renew the authenticator and return the response with a new JWT if DAO is enabled" in new WithApplication with Context {
      implicit val request = FakeRequest()
      val id = "new-test-id"

      repository.remove(any()) answers { _: Any => Future.successful(()) }
      repository.add(any()) answers { p: Any => Future.successful(p.asInstanceOf[JWTAuthenticator]) }
      idGenerator.generate returns Future.successful(id)
      clock.now returns ZonedDateTime.now.`with`(ChronoField.MILLI_OF_SECOND, 0)

      val result = service(Some(repository)).renew(authenticator, Results.Ok)

      unserialize(header(settings.fieldName, result).get, authenticatorEncoder, settings).get must be equalTo authenticator.copy(
        id = id,
        expirationDateTime = clock.now + settings.authenticatorExpiry,
        lastUsedDateTime = clock.now)

      there was one(repository).add(any())
      there was one(repository).remove(authenticator.id)
    }

    "renew an authenticator with custom claims" in new WithApplication with Context {
      implicit val request = FakeRequest()
      val id = "new-test-id"

      repository.remove(any()) returns Future.successful(())
      repository.add(any()) answers { p: Any => Future.successful(p.asInstanceOf[JWTAuthenticator]) }
      idGenerator.generate returns Future.successful(id)
      clock.now returns ZonedDateTime.now.`with`(ChronoField.MILLI_OF_SECOND, 0)

      val result = service(Some(repository)).renew(authenticator.copy(customClaims = Some(customClaims)), Results.Ok)

      unserialize(header(settings.fieldName, result).get, authenticatorEncoder, settings).get must be equalTo authenticator.copy(
        id = id,
        expirationDateTime = clock.now + settings.authenticatorExpiry,
        lastUsedDateTime = clock.now,
        customClaims = Some(customClaims))

      there was one(repository).add(any())
      there was one(repository).remove(authenticator.id)
    }

    "renew the authenticator and return the response with a new JWT if DAO is disabled" in new WithApplication with Context {
      implicit val request = FakeRequest()
      val id = "new-test-id"

      idGenerator.generate returns Future.successful(id)
      clock.now returns ZonedDateTime.now.`with`(ChronoField.MILLI_OF_SECOND, 0)

      val result = service(None).renew(authenticator, Results.Ok)

      unserialize(header(settings.fieldName, result).get, authenticatorEncoder, settings).get must be equalTo authenticator.copy(
        id = id,
        expirationDateTime = clock.now + settings.authenticatorExpiry,
        lastUsedDateTime = clock.now)
      there was no(repository).remove(any())
      there was no(repository).add(any())
    }

    "throws an AuthenticatorRenewalException exception if an error occurred during renewal" in new Context {
      implicit val request = FakeRequest()
      val now = ZonedDateTime.now
      val id = "new-test-id"

      repository.remove(any()) returns Future.successful(())
      repository.add(any()) returns Future.failed(new Exception("Cannot store authenticator"))
      idGenerator.generate returns Future.successful(id)
      clock.now returns now

      await(service(Some(repository)).renew(authenticator, Results.Ok)) must throwA[AuthenticatorRenewalException].like {
        case e =>
          e.getMessage must startWith(RenewError.format(ID, ""))
      }
    }
  }

  "The `discard` method of the service" should {
    "remove authenticator from backing store if DAO is enabled" in new Context {
      implicit val request = FakeRequest()

      repository.remove(authenticator.id) returns Future.successful(authenticator)

      await(service(Some(repository)).discard(authenticator, Results.Ok))

      there was one(repository).remove(authenticator.id)
    }

    "do not remove the authenticator from backing store if DAO is disabled" in new Context {
      implicit val request = FakeRequest()

      await(service(None).discard(authenticator, Results.Ok))

      there was no(repository).remove(authenticator.id)
    }

    "throws an AuthenticatorDiscardingException exception if an error occurred during discarding" in new Context {
      implicit val request = FakeRequest()
      val okResult = Results.Ok

      repository.remove(authenticator.id) returns Future.failed(new Exception("Cannot remove authenticator"))

      await(service(Some(repository)).discard(authenticator, okResult)) must throwA[AuthenticatorDiscardingException].like {
        case e =>
          e.getMessage must startWith(DiscardError.format(ID, ""))
      }
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    private val lastUsedDateTime = ZonedDateTime.now.`with`(ChronoField.MILLI_OF_SECOND, 0)

    /**
     * The repository implementation.
     */
    lazy val repository = mock[AuthenticatorRepository[JWTAuthenticator]].smart

    /**
     * The authenticator encoder implementation.
     */
    lazy val authenticatorEncoder = new Base64AuthenticatorEncoder

    /**
     * The ID generator implementation.
     */
    lazy val idGenerator = mock[IDGenerator].smart

    /**
     * The clock implementation.
     */
    lazy val clock: Clock = mock[Clock].smart

    /**
     * The settings.
     */
    lazy val settings = spy(JWTAuthenticatorSettings(
      fieldName = "X-Auth-Token",
      issuerClaim = "play-silhouette",
      authenticatorIdleTimeout = Some(30 minutes),
      authenticatorExpiry = 12 hours,
      sharedSecret = "fGhre3$56%43erfkl8)/§$dsdf345gsdfvsdf23kl"))

    /**
     * The authenticator service instance to test.
     */
    lazy val service = (repository: Option[AuthenticatorRepository[JWTAuthenticator]]) =>
      new JWTAuthenticatorService(settings, repository, authenticatorEncoder, idGenerator, clock)

    /**
     * The login info.
     */
    lazy val loginInfo = LoginInfo("test", "1")

    /**
     * An authenticator.
     */
    lazy val authenticator = new JWTAuthenticator(
      id = "test-id",
      loginInfo = LoginInfo("test", "1"),
      lastUsedDateTime = lastUsedDateTime,
      expirationDateTime = lastUsedDateTime + settings.authenticatorExpiry,
      idleTimeout = settings.authenticatorIdleTimeout)

    /**
     * Some custom claims.
     */
    lazy val customClaims: JsObject = Json.obj(
      fields =
        "boolean" -> true,
      "string" -> "string",
      "number" -> 1234567890,
      "array" -> Json.arr(items = 1, 2),
      "object" -> Json.obj(
        fields =
          "array" -> Seq("string1", "string2"),
        "object" -> Json.obj(fields =
          "array" -> Json.arr(items = "string", false, Json.obj(fields = "number" -> 1)))))
  }
}
