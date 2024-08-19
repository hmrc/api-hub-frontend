package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class RegisterApplicationTeamSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "RegisterApplicationTeam" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(RegisterApplicationTeam.values.toSeq)

      forAll(gen) {
        registerApplicationTeam =>

          JsString(registerApplicationTeam.toString).validate[RegisterApplicationTeam].asOpt.value mustEqual registerApplicationTeam
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!RegisterApplicationTeam.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[RegisterApplicationTeam] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(RegisterApplicationTeam.values.toSeq)

      forAll(gen) {
        registerApplicationTeam =>

          Json.toJson(registerApplicationTeam) mustEqual JsString(registerApplicationTeam.toString)
      }
    }
  }
}
