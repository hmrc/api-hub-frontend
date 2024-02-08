package utils

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.libs.json.{Format, Json}

import java.time.{Instant, LocalDateTime, OffsetDateTime}

class HIPP984Spec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  private case class LocalDateTimeApi(id: String, lastDeployed: LocalDateTime)
  private implicit val formatLocalDateTimeApi: Format[LocalDateTimeApi] = Json.format[LocalDateTimeApi]

  private case class OffsetDateTimeApi(id: String, lastDeployed: OffsetDateTime)
  private implicit val formatOffsetDateTimeApi: Format[OffsetDateTimeApi] = Json.format[OffsetDateTimeApi]

  private case class InstantApi(id: String, lastDeployed: Instant)
  private implicit val formatInstantApi: Format[InstantApi] = Json.format[InstantApi]

  private val dates = Table(
    "Date",
    "2024-02-05T07:53:32Z",
    "2024-02-05T07:53:32+00:00",
    "2024-02-05T07:53:32-07:00",
    "2024-02-05T07:53:32+02:00"
  )

  "LocalDateTime" - {

    "must read/write a timestamp correctly" in {
      forAll(dates) { (date: String) =>
        val api = Json.parse(buildJson(date)).as[LocalDateTimeApi]
        println(Json.toJson(api).toString())
      }
    }

  }

  "OffsetDateTime" - {

    "must read/write a timestamp correctly" in {
      forAll(dates) { (date: String) =>
        val api = Json.parse(buildJson(date)).as[OffsetDateTimeApi]
        println(Json.toJson(api).toString())
      }
    }

  }

  "Instant" - {

    "must read/write a timestamp correctly" in {
      forAll(dates) { (date: String) =>
        val api = Json.parse(buildJson(date)).as[InstantApi]
        println(Json.toJson(api).toString())
      }
    }

  }

  private def buildJson(timestamp: String): String = {
    s"{\"id\":\"test-id\",\"lastDeployed\":\"$timestamp\"}"
  }

}
