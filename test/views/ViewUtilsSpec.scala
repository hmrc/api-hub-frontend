/*
 * Copyright 2023 HM Revenue & Customs
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

package views

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import java.time.{LocalDateTime, ZoneOffset}

class ViewUtilsSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  "formatLocalDateTimeContainingUtc" - {
    "must return a date/time in the correct format for GMT time" in {
      //noinspection ScalaStyle
      val dateTime = LocalDateTime.of(2023, 1, 3, 4, 5, 6)
      val expected = "2023-01-03T04:05:06"

      val actual = ViewUtils.formatLocalDateTimeContainingUtc(dateTime)
      actual mustBe expected
    }

    "must return a date/time in the correct format for BST time" in {
      //noinspection ScalaStyle
      val dateTime = LocalDateTime.of(2023, 7, 3, 4, 5, 6)
      val expected = "2023-07-03T04:05:06"

      val actual = ViewUtils.formatLocalDateTimeContainingUtc(dateTime)
      actual mustBe expected
    }
  }

  "formatInstantAsUtc" - {
    "must return a date/time in the correct format for GMT time" in {
      //noinspection ScalaStyle
      val instant = LocalDateTime.of(2023, 1, 3, 4, 5, 6).toInstant(ZoneOffset.UTC)
      val expected = "2023-01-03T04:05:06Z"

      val actual = ViewUtils.formatInstantAsUtc(instant)
      actual mustBe expected
    }

    "must return a date/time in the correct format for BST time" in {
      //noinspection ScalaStyle
      val instant = LocalDateTime.of(2023, 7, 3, 4, 5, 6).toInstant(ZoneOffset.UTC)
      val expected = "2023-07-03T04:05:06Z"

      val actual = ViewUtils.formatInstantAsUtc(instant)
      actual mustBe expected
    }
  }
  
  "addFormattingMarkup" - {
    "must return a string with the correct HTML markup" in {
      val testCases = Table(
        ("message text", "expected HTML"),
        ("", ""),
        ("text", "text"),
        ("some _text_", "some _text_"),
        ("some __text__", "some <strong>text</strong>"),
        ("some ____", "some ____"),
        ("some __text", "some __text"),
        ("some __text__ with __multiple__ __bold__ words", "some <strong>text</strong> with <strong>multiple</strong> <strong>bold</strong> words"),
      )

      forAll(testCases) { (messageText: String, expectedHtml: String) =>
        ViewUtils.addFormattingMarkup(messageText).toString mustBe expectedHtml
      }
    }
  }
}
