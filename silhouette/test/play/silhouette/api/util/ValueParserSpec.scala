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

import org.specs2.mutable.Specification

class ValueParserSpec extends Specification {

  "DefaultValueParser" should {
    "return the raw value wrapped in Some" in {
      DefaultValueParser.parseValue("abc123") must beSome("abc123")
    }
  }

  "BearerValueParser" should {
    "return the token without Bearer prefix" in {
      BearerValueParser.parseValue("Bearer abc123") must beSome("abc123")
    }

    "trim whitespace after Bearer prefix" in {
      BearerValueParser.parseValue("Bearer     xyz789") must beSome("xyz789")
    }

    "return None if prefix is missing" in {
      BearerValueParser.parseValue("xyz789") must beNone
    }

    "return None if input is just 'Bearer'" in {
      BearerValueParser.parseValue("Bearer") must beNone
    }

    "return None if prefix is case-sensitive mismatch" in {
      BearerValueParser.parseValue("bearer abc123") must beNone
    }
  }
}
