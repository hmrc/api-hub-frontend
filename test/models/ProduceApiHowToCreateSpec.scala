package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class ProduceApiHowToCreateSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "ProduceApiHowToCreate" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(ProduceApiHowToCreate.values.toSeq)

      forAll(gen) {
        produceApiHowToCreate =>

          JsString(myApis.produce.howtocreate.toString).validate[ProduceApiHowToCreate].asOpt.value mustEqual produceApiHowToCreate
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!ProduceApiHowToCreate.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[ProduceApiHowToCreate] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(ProduceApiHowToCreate.values.toSeq)

      forAll(gen) {
        produceApiHowToCreate =>

          Json.toJson(produceApiHowToCreate) mustEqual JsString(myApis.produce.howtocreate.toString)
      }
    }
  }
}
