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
package  play.silhouette.impl.util

import org.mockito.Mockito._
import org.specs2.specification.Scope
import play.api.cache.AsyncCacheApi
import play.api.test.PlaySpecification

import java.time.ZonedDateTime
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 * Test case for the [[play.silhouette.impl.util.PlayCacheLayer]] class.
 */
class PlayCacheLayerSpec extends PlaySpecification {

  "The `find` method" should {
    "return value from cache" in new Context {
      when(cacheAPI.get[ZonedDateTime]("id")).thenReturn(Future.successful(Some(value)))

      await(layer.find[ZonedDateTime]("id")) should beSome(value)

      verify(cacheAPI).get[ZonedDateTime]("id")
    }
  }

  "The `save` method" should {
    "save value in cache" in new Context {
      await(layer.save("id", value))

      verify(cacheAPI).set("id", value, Duration.Inf)
    }
  }

  "The `remove` method" should {
    "removes value from cache" in new Context {
      await(layer.remove("id")) must beEqualTo(())

      verify(cacheAPI).remove("id")
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The cache API.
     */
    lazy val cacheAPI = mock(classOf[AsyncCacheApi])

    /**
     * The layer to test.
     */
    lazy val layer = new PlayCacheLayer(cacheAPI)

    /**
     * The value to cache.
     */
    lazy val value = ZonedDateTime.now
  }
}
