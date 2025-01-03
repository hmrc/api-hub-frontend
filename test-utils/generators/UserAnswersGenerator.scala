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

package generators

import models.UserAnswers
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.TryValues
import pages.*
import pages.application.accessrequest.RequestProductionAccessSelectApisPage
import pages.application.cancelaccessrequest.CancelAccessRequestConfirmPage
import pages.application.register.RegisterApplicationTeamPage
import pages.myapis.produce.*
import play.api.libs.json.{JsValue, Json}

trait UserAnswersGenerator extends TryValues {
  self: Generators =>

  val generators: Seq[Gen[(QuestionPage[?], JsValue)]] =
    arbitrary[(ProduceApiHowToAddWiremockPage.type, JsValue)] ::
    arbitrary[(ProduceApiPassthroughPage.type, JsValue)] ::
    arbitrary[(ProduceApiEgressPrefixesPage.type, JsValue)] ::
    arbitrary[(ProduceApiDomainPage.type, JsValue)] ::
    arbitrary[(ProduceApiShortDescriptionPage.type, JsValue)] ::
    arbitrary[(ProduceApiReviewNameDescriptionPage.type, JsValue)] ::
    arbitrary[(ProduceApiChooseTeamPage.type, JsValue)] ::
    arbitrary[(CancelAccessRequestConfirmPage.type, JsValue)] ::
    arbitrary[(RequestProductionAccessSelectApisPage.type, JsValue)] ::
    arbitrary[(ProduceApiEnterOasPage.type, JsValue)] ::
    arbitrary[(ProduceApiHowToCreatePage.type, JsValue)] ::
    arbitrary[(RegisterApplicationTeamPage.type, JsValue)] ::
    arbitrary[(CreateTeamNamePage.type, JsValue)] ::
    arbitrary[(ApiPolicyConditionsDeclarationPage.type, JsValue)] ::
    arbitrary[(AddAnApiSelectEndpointsPage.type, JsValue)] ::
    Nil

  implicit lazy val arbitraryUserData: Arbitrary[UserAnswers] = {

    import models._

    Arbitrary {
      for {
        id      <- nonEmptyString
        data    <- generators match {
          case Nil => Gen.const(Map[QuestionPage[?], JsValue]())
          case _   => Gen.mapOf(oneOf(generators))
        }
      } yield UserAnswers (
        id = id,
        data = data.foldLeft(Json.obj()) {
          case (obj, (path, value)) =>
            obj.setObject(path.path, value).get
        }
      )
    }
  }
}
