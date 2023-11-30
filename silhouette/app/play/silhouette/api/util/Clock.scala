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
package  play.silhouette.api.util

import java.time.{ Instant, ZonedDateTime, ZoneId, Clock => JavaClock }

/**
 * A trait which provides a mockable implementation for a Clock instance.
 */
trait Clock extends JavaClock {

  /**
   * Gets the current DateTime.
   *
   * @return the current DateTime.
   */
  def now: ZonedDateTime
}

/**
 * Creates a clock implementation.
 */
object Clock {

  /**
   * Gets a Clock implementation.
   *
   * @return A Clock implementation.
   */
  def apply(): Clock = Clock(JavaClock.systemDefaultZone)

  def apply(clock: JavaClock): Clock = new Clock {
    def now: ZonedDateTime = ZonedDateTime.now(clock)

    def getZone: ZoneId = clock.getZone

    override def withZone(zone: ZoneId): Clock = Clock(clock.withZone(zone))

    def instant(): Instant = clock.instant
  }
}
