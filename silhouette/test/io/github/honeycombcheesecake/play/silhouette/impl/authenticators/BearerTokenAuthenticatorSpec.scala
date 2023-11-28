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

import io.github.honeycombcheesecake.play.silhouette.api.Authenticator.Implicits._
import io.github.honeycombcheesecake.play.silhouette.api.LoginInfo
import io.github.honeycombcheesecake.play.silhouette.api.exceptions._
import io.github.honeycombcheesecake.play.silhouette.api.repositories.AuthenticatorRepository
import io.github.honeycombcheesecake.play.silhouette.api.services.AuthenticatorService._
import io.github.honeycombcheesecake.play.silhouette.api.util.{ RequestPart, Clock, IDGenerator }
import io.github.honeycombcheesecake.play.silhouette.impl.authenticators.BearerTokenAuthenticatorService._
import org.specs2.specification.Scope
import play.api.mvc.{ Results, AnyContentAsEmpty }
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import test.Helper.mockSmart

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Test case for the [[io.github.honeycombcheesecake.play.silhouette.impl.authenticators.BearerTokenAuthenticator]].
 */
class BearerTokenAuthenticatorSpec extends PlaySpecification {

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

  "The `create` method of the service" should {
    "return an authenticator with the generated ID" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val id = "test-id"

      when(idGenerator.generate).thenReturn(Future.successful(id))
      when(clock.now).thenReturn(ZonedDateTime.now)

      await(service.create(loginInfo)).id must be equalTo id
    }

    "return an authenticator with the current date as lastUsedDateTime" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val now = ZonedDateTime.now

      when(idGenerator.generate).thenReturn(Future.successful("test-id"))
      when(clock.now).thenReturn(now)

      await(service.create(loginInfo)).lastUsedDateTime must be equalTo now
    }

    "return an authenticator which expires in 12 hours(default value)" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val now = ZonedDateTime.now

      when(idGenerator.generate).thenReturn(Future.successful("test-id"))
      when(clock.now).thenReturn(now)

      await(service.create(loginInfo)).expirationDateTime must be equalTo now + 12.hours
    }

    "return an authenticator which expires in 6 hours" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val sixHours = 6 hours
      val now = ZonedDateTime.now

      when(settings.authenticatorExpiry).thenReturn(sixHours)
      when(idGenerator.generate).thenReturn(Future.successful("test-id"))
      when(clock.now).thenReturn(now)

      await(service.create(loginInfo)).expirationDateTime must be equalTo now + sixHours
    }

    "throws an AuthenticatorCreationException exception if an error occurred during creation" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      when(idGenerator.generate).thenReturn(Future.failed(new Exception("Could not generate ID")))

      await(service.create(loginInfo)) must throwA[AuthenticatorCreationException].like {
        case e =>
          e.getMessage must startWith(CreateError.format(ID, ""))
      }
    }
  }

  "The `retrieve` method of the service" should {
    "return None if no authenticator header exists" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      await(service.retrieve) must beNone
    }

    "return None if no authenticator is stored for the token located in the headers" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(settings.fieldName -> authenticator.id)

      when(repository.find(authenticator.id)).thenReturn(Future.successful(None))

      await(service.retrieve) must beNone
    }

    "return authenticator if an authenticator is stored for token located in the header" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(settings.fieldName -> authenticator.id)

      when(repository.find(authenticator.id)).thenReturn(Future.successful(Some(authenticator)))

      await(service.retrieve) must beSome(authenticator)
    }

    "return authenticator if an authenticator is stored for the token located in the query string" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", s"?${settings.fieldName}=${authenticator.id}")

      when(settings.requestParts).thenReturn(Some(Seq(RequestPart.QueryString)))
      when(repository.find(authenticator.id)).thenReturn(Future.successful(Some(authenticator)))

      await(service.retrieve) must beSome(authenticator)
    }

    "throws an AuthenticatorRetrievalException exception if an error occurred during retrieval" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(settings.fieldName -> authenticator.id)

      when(repository.find(authenticator.id)).thenReturn(Future.failed(new RuntimeException("Cannot find authenticator")))

      await(service.retrieve) must throwA[AuthenticatorRetrievalException].like {
        case e =>
          e.getMessage must startWith(RetrieveError.format(ID, ""))
      }
    }
  }

  "The `init` method of the service" should {
    "save the authenticator in backing store" in new Context {
      when(repository.add(any())).thenAnswer { p => Future.successful(p.getArgument(0).asInstanceOf[BearerTokenAuthenticator]) }

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val token = await(service.init(authenticator))

      token must be equalTo authenticator.id
      verify(repository).add(authenticator)
    }

    "throws an AuthenticatorInitializationException exception if an error occurred during initialization" in new Context {
      when(repository.add(any())).thenReturn(Future.failed(new Exception("Cannot store authenticator")))

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      await(service.init(authenticator)) must throwA[AuthenticatorInitializationException].like {
        case e =>
          e.getMessage must startWith(InitError.format(ID, ""))
      }
    }
  }

  "The result `embed` method of the service" should {
    "return the response with a header" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val value = authenticator.id
      val result = service.embed(value, Results.Ok)

      header(settings.fieldName, result) should beSome(authenticator.id)
    }
  }

  "The request `embed` method of the service" should {
    "return the request with a header" in new Context {
      val value = authenticator.id
      val request = service.embed(value, FakeRequest())

      request.headers.get(settings.fieldName) should beSome(authenticator.id)
    }

    "override an existing header" in new Context {
      val value = authenticator.id
      val request = service.embed(value, FakeRequest().withHeaders(settings.fieldName -> "test"))

      request.headers.get(settings.fieldName) should beSome(authenticator.id)
    }

    "keep non authenticator related headers" in new Context {
      val value = authenticator.id
      val request = service.embed(value, FakeRequest().withHeaders("test" -> "test"))

      request.headers.get(settings.fieldName) should beSome(authenticator.id)
      request.headers.get("test") should beSome("test")
    }

    "keep other request parts" in new Context {
      val value = authenticator.id
      val request = service.embed(value, FakeRequest().withSession("test" -> "test"))

      request.headers.get(settings.fieldName) should beSome(authenticator.id)
      request.session.get("test") should beSome("test")
    }
  }

  "The `touch` method of the service" should {
    "update the last used date if idle timeout is defined" in new WithApplication with Context {
      override def running() = {
        when(settings.authenticatorIdleTimeout).thenReturn(Some(1 second))
        when(clock.now).thenReturn(ZonedDateTime.now)

        service.touch(authenticator) must beLeft[BearerTokenAuthenticator].like {
          case a =>
            a.lastUsedDateTime must be equalTo clock.now
        }
      }
    }

    "do not update the last used date if idle timeout is not defined" in new WithApplication with Context {
      override def running() = {
        when(settings.authenticatorIdleTimeout).thenReturn(None)
        when(clock.now).thenReturn(ZonedDateTime.now)

        service.touch(authenticator) must beRight[BearerTokenAuthenticator].like {
          case a =>
            a.lastUsedDateTime must be equalTo authenticator.lastUsedDateTime
        }
      }
    }
  }

  "The `update` method of the service" should {
    "update the authenticator in backing store" in new Context {
      when(repository.update(any())).thenAnswer { _ => Future.successful(authenticator) }

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      await(service.update(authenticator, Results.Ok))

      verify(repository).update(authenticator)
    }

    "return the result if the authenticator could be stored in backing store" in new Context {
      when(repository.update(any())).thenAnswer { p => Future.successful(p.getArgument(0).asInstanceOf[BearerTokenAuthenticator]) }

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val result = service.update(authenticator, Results.Ok)

      status(result) must be equalTo OK
    }

    "throws an AuthenticatorUpdateException exception if an error occurred during update" in new Context {
      when(repository.update(any())).thenReturn(Future.failed(new Exception("Cannot store authenticator")))

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      await(service.update(authenticator, Results.Ok)) must throwA[AuthenticatorUpdateException].like {
        case e =>
          e.getMessage must startWith(UpdateError.format(ID, ""))
      }
    }
  }

  "The `renew` method of the service" should {
    "remove the old authenticator from backing store" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val now = ZonedDateTime.now
      val id = "new-test-id"

      when(repository.remove(authenticator.id)).thenReturn(Future.successful(()))
      when(repository.add(any())).thenAnswer { p => Future.successful(p.getArgument(0).asInstanceOf[BearerTokenAuthenticator]) }
      when(idGenerator.generate).thenReturn(Future.successful(id))
      when(clock.now).thenReturn(now)

      await(service.renew(authenticator, Results.Ok))

      verify(repository).remove(authenticator.id)
    }

    "renew the authenticator and return the response with a new bearer token" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val now = ZonedDateTime.now
      val id = "new-test-id"

      when(repository.remove(any())).thenReturn(Future.successful(()))
      when(repository.add(any())).thenAnswer { p => Future.successful(p.getArgument(0).asInstanceOf[BearerTokenAuthenticator]) }
      when(idGenerator.generate).thenReturn(Future.successful(id))
      when(clock.now).thenReturn(now)

      val result = service.renew(authenticator, Results.Ok)

      header(settings.fieldName, result) should beSome(id)
    }

    "throws an AuthenticatorRenewalException exception if an error occurred during renewal" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val now = ZonedDateTime.now
      val id = "new-test-id"

      when(repository.remove(any())).thenReturn(Future.successful(()))
      when(repository.add(any())).thenReturn(Future.failed(new Exception("Cannot store authenticator")))
      when(idGenerator.generate).thenReturn(Future.successful(id))
      when(clock.now).thenReturn(now)

      await(service.renew(authenticator, Results.Ok)) must throwA[AuthenticatorRenewalException].like {
        case e =>
          e.getMessage must startWith(RenewError.format(ID, ""))
      }
    }
  }

  "The `discard` method of the service" should {
    "remove authenticator from backing store" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      when(repository.remove(authenticator.id)).thenReturn(Future.successful(authenticator))

      await(service.discard(authenticator, Results.Ok))

      verify(repository).remove(authenticator.id)
    }

    "throws an AuthenticatorDiscardingException exception if an error occurred during discarding" in new Context {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val okResult = Results.Ok

      when(repository.remove(authenticator.id)).thenReturn(Future.failed(new Exception("Cannot remove authenticator")))

      await(service.discard(authenticator, okResult)) must throwA[AuthenticatorDiscardingException].like {
        case e =>
          e.getMessage must startWith(DiscardError.format(ID, ""))
      }
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The repository implementation.
     */
    lazy val repository = mockSmart[AuthenticatorRepository[BearerTokenAuthenticator]]

    /**
     * The ID generator implementation.
     */
    lazy val idGenerator = mockSmart[IDGenerator]

    /**
     * The clock implementation.
     */
    lazy val clock = mockSmart[Clock]

    /**
     * The settings.
     */
    lazy val settings = spy(BearerTokenAuthenticatorSettings(
      fieldName = "X-Auth-Token",
      authenticatorIdleTimeout = Some(30 minutes),
      authenticatorExpiry = 12 hours))

    /**
     * The authenticator service instance to test.
     */
    lazy val service = new BearerTokenAuthenticatorService(settings, repository, idGenerator, clock)

    /**
     * The login info.
     */
    lazy val loginInfo = LoginInfo("test", "1")

    /**
     * An authenticator.
     */
    lazy val authenticator = new BearerTokenAuthenticator(
      id = "test-id",
      loginInfo = LoginInfo("test", "1"),
      lastUsedDateTime = ZonedDateTime.now,
      expirationDateTime = ZonedDateTime.now + settings.authenticatorExpiry,
      idleTimeout = settings.authenticatorIdleTimeout)
  }
}
