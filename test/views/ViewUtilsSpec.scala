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

import java.time.LocalDateTime

class ViewUtilsSpec extends AnyFreeSpec with Matchers {

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

}
