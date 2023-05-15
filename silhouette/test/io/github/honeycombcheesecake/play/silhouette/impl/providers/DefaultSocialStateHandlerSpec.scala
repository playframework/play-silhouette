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
package io.github.honeycombcheesecake.play.silhouette.impl.providers

import io.github.honeycombcheesecake.play.silhouette.api.crypto.Signer
import io.github.honeycombcheesecake.play.silhouette.api.exceptions.ProviderException
import io.github.honeycombcheesecake.play.silhouette.api.util.ExtractableRequest
import io.github.honeycombcheesecake.play.silhouette.impl.providers.DefaultSocialStateHandler._
import io.github.honeycombcheesecake.play.silhouette.impl.providers.SocialStateItem.ItemStructure
import org.specs2.matcher.JsonMatchers
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.{ FakeRequest, PlaySpecification }
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import test.Helper.mockSmart

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success }

/**
 *  Test case for the [[DefaultSocialStateHandler]] class.
 */
class DefaultSocialStateHandlerSpec extends PlaySpecification with JsonMatchers {

  "The `withHandler` method" should {
    "return a new state handler with the given item handler added" in new Context {
      val newHandler = mockSmart[SocialStateItemHandler]

      stateHandler.handlers.size must be equalTo 2
      stateHandler.withHandler(newHandler).handlers.size must be equalTo 3
    }
  }

  "The `state` method" should {
    "return the social state" in new Context {
      when(Default.itemHandler.item).thenReturn(Future.successful(Default.item))
      when(Publishable.itemHandler.item).thenReturn(Future.successful(Publishable.item))

      await(stateHandler.state) must be equalTo state
    }
  }

  "The `serialize` method" should {
    "return an empty string if no handler is registered" in new Context {
      override val stateHandler = new DefaultSocialStateHandler(Set(), signer)

      stateHandler.serialize(state) must be equalTo ""
    }

    "return an empty string if the items are empty" in new Context {
      stateHandler.serialize(SocialState(Set())) must be equalTo ""
    }

    "return the serialized social state" in new Context {
      when(Default.itemHandler.canHandle(Publishable.item)).thenReturn(None)
      when(Default.itemHandler.canHandle(Default.item)).thenReturn(Some(Default.item))
      when(Default.itemHandler.serialize(Default.item)).thenReturn(Default.structure)

      when(Publishable.itemHandler.canHandle(Default.item)).thenReturn(None)
      when(Publishable.itemHandler.canHandle(Publishable.item)).thenReturn(Some(Publishable.item))
      when(Publishable.itemHandler.serialize(Publishable.item)).thenReturn(Publishable.structure)

      stateHandler.serialize(state) must be equalTo s"${Publishable.structure.asString}.${Default.structure.asString}"
    }
  }

  "The `unserialize` method" should {
    "omit state validation if no handler is registered" in new Context {
      override val stateHandler = new DefaultSocialStateHandler(Set(), signer)
      implicit val request = new ExtractableRequest(FakeRequest())

      await(stateHandler.unserialize(""))

      verify(signer, never()).extract(any[String]())
    }

    "throw an Exception for an empty string" in new Context {
      implicit val request = new ExtractableRequest(FakeRequest())

      await(stateHandler.unserialize("")) must throwA[RuntimeException].like {
        case e =>
          e.getMessage must startWith("Wrong state format")
      }
    }

    "throw an ProviderException if the serialized item structure cannot be extracted" in new Context {
      implicit val request = new ExtractableRequest(FakeRequest())
      val serialized = s"some-wired-content"

      await(stateHandler.unserialize(serialized)) must throwA[ProviderException].like {
        case e =>
          e.getMessage must startWith(ItemExtractionError.format(serialized))
      }
    }

    "throw an ProviderException if none of the item handlers can handle the given state" in new Context {
      implicit val request = new ExtractableRequest(FakeRequest())
      val serialized = s"${Default.structure.asString}"

      when(Default.itemHandler.canHandle(any[ItemStructure]())(any())).thenReturn(false)
      when(Publishable.itemHandler.canHandle(any[ItemStructure]())(any())).thenReturn(false)

      await(stateHandler.unserialize(serialized)) must throwA[ProviderException].like {
        case e =>
          e.getMessage must startWith(MissingItemHandlerError.format(Default.structure))
      }
    }

    "return the unserialized social state" in new Context {
      implicit val request = new ExtractableRequest(FakeRequest())
      val serialized = s"${Default.structure.asString}.${Publishable.structure.asString}"

      when(Default.itemHandler.canHandle(Publishable.structure)).thenReturn(false)
      when(Default.itemHandler.canHandle(Default.structure)).thenReturn(true)
      when(Default.itemHandler.unserialize(Default.structure)).thenReturn(Future.successful(Default.item))

      when(Publishable.itemHandler.canHandle(Default.structure)).thenReturn(false)
      when(Publishable.itemHandler.canHandle(Publishable.structure)).thenReturn(true)
      when(Publishable.itemHandler.unserialize(Publishable.structure)).thenReturn(Future.successful(Publishable.item))

      await(stateHandler.unserialize(serialized)) must be equalTo SocialState(Set(Default.item, Publishable.item))
    }
  }

  "The `publish` method" should {
    "should publish the state with the publishable handler that is responsible for the item" in new Context {
      implicit val request = new ExtractableRequest(FakeRequest())
      val result = Results.Ok
      val publishedResult = Results.Ok.withHeaders("X-PUBLISHED" -> "true")

      when(Publishable.itemHandler.publish(Publishable.item, result)).thenReturn(publishedResult)
      when(Publishable.itemHandler.canHandle(Default.item)).thenReturn(None)
      when(Publishable.itemHandler.canHandle(Publishable.item)).thenReturn(Some(Publishable.item))

      stateHandler.publish(result, state) must be equalTo publishedResult
    }

    "should not publish the state if no publishable handler is responsible" in new Context {
      implicit val request = FakeRequest()
      val result = Results.Ok

      when(Publishable.itemHandler.canHandle(Default.item)).thenReturn(None)
      when(Publishable.itemHandler.canHandle(Publishable.item)).thenReturn(None)

      stateHandler.publish(result, state) must be equalTo result
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A default handler implementation.
     */
    case class DefaultItem() extends SocialStateItem
    trait DefaultItemHandler extends SocialStateItemHandler {
      type Item = DefaultItem
    }
    object Default {
      val itemHandler = mockSmart[DefaultItemHandler]
      val item = DefaultItem()
      val structure = ItemStructure("default", Json.obj())
    }

    /**
     * A publishable handler implementation.
     */
    case class PublishableItem() extends SocialStateItem
    trait PublishableItemHandler extends SocialStateItemHandler with PublishableSocialStateItemHandler {
      type Item = PublishableItem
    }
    object Publishable {
      val itemHandler = mockSmart[PublishableItemHandler]
      val item = PublishableItem()
      val structure = ItemStructure("publishable", Json.obj())
    }

    /**
     * The signer implementation.
     *
     * The signer returns the same value as passed to the methods. This is enough for testing.
     */
    val signer = {
      val c = mockSmart[Signer]
      when(c.sign(any())).thenAnswer(_.getArgument(0).asInstanceOf[String])
      when(c.extract(any())).thenAnswer { p =>
        p.getArgument(0).asInstanceOf[String] match {
          case "" => Failure(new RuntimeException("Wrong state format"))
          case s => Success(s)
        }
      }
      c
    }

    /**
     * The state.
     */
    val state = SocialState(Set(Publishable.item, Default.item))

    /**
     * The state handler to test.
     */
    val stateHandler = new DefaultSocialStateHandler(Set(Publishable.itemHandler, Default.itemHandler), signer)
  }
}
