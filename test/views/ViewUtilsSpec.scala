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
import play.api.test.FakeRequest

import java.time.{LocalDateTime, ZoneOffset}

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

  "linkTarget" - {
    val microsoftBrowsersAgents = Seq(
      "Mozilla/5.0 (Windows Phone 10.0; Android 6.0.1; Microsoft; RM-1152) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Mobile Safari/537.36 Edge/15.15254",
      "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; Media Center PC 6.0; InfoPath.3; MS-RTC LM 8; Zune 4.7)",
    )
    val nonMicrosoftBrowsersAgents = Seq(
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.5112.79 Safari/537.36",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:99.0) Gecko/20100101 Firefox/99.0",
      "Opera/9.80 (Windows NT 6.0) Presto/2.12.388 Version/12.14"
    )
    "must return '_self' on microsoft browsers" in {
      val result = microsoftBrowsersAgents.map(ua =>
        ViewUtils.linkTarget(FakeRequest().withHeaders("User-Agent" -> ua))
      )

      all(result) mustBe "_self"
    }
    "must return '_blank' on non-microsoft browsers" in {
      val result = nonMicrosoftBrowsersAgents.map(ua =>
        ViewUtils.linkTarget(FakeRequest().withHeaders("User-Agent" -> ua))
      )

      all(result) mustBe "_blank"
    }
  }
}
