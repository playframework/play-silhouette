/**
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>
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
package play.silhouette.api.util

trait ValueParser {

  /**
   * Parses a value from a field, be it headers, JSON body, etc.
   *
   * @param raw The raw value to parse
   * @return The parsed value
   */
  def parseValue(raw: String): Option[String]
}

object DefaultValueParser extends ValueParser {

  /**
   * Parses a value from a field, be it headers, JSON body, etc. This is the default [[ValueParser]], and just returns the string as an option.
   *
   * @param raw The raw value to parse
   * @return The parsed value
   */
  def parseValue(raw: String): Option[String] = Some(raw)

}

object BearerValueParser extends ValueParser {

  /**
   * Parses a value from a field, be it headers, JSON body, etc.
   *
   * @param raw The raw value to parse
   * @return The parsed value
   */
  def parseValue(raw: String): Option[String] = {
    if (raw.startsWith("Bearer ")) Some(raw.stripPrefix("Bearer ").trim) else None
  }
}
