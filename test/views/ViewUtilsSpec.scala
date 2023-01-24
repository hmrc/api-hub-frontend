package views

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.LocalDateTime

class ViewUtilsSpec extends AnyFreeSpec with Matchers {

  "formatLocalDateTime" - {
    "must return a date/time in the correct format" in {
      //noinspection ScalaStyle
      val dateTime = LocalDateTime.of(2023, 1, 3, 4, 5, 6)
      val expected = "3 January 2023 04:05"

      val actual = ViewUtils.formatLocalDateTime(dateTime)
      actual mustBe expected
    }
  }

}
