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

package controllers.actions

import models.requests.DataRequest
import models.{AddAnApi, AddAnApiContext, AddEndpoints, UserAnswers}
import org.scalatest.OptionValues
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import pages.AddAnApiContextPage
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import play.api.test.FakeRequest

import scala.concurrent.Future

class AddAnApiCheckContextActionSpec extends AsyncFreeSpec with Matchers with OptionValues with TableDrivenPropertyChecks {

  import AddAnApiCheckContextActionSpec._

  "AddAnApiCheckContextAction" - {
    "must process the request correctly when the context is correct" in {
      val contexts = Table(
        "Context",
        AddAnApi,
        AddEndpoints
      )

      forAll(contexts) {(context: AddAnApiContext) =>
        val userAnswers = UserAnswers("id").set(AddAnApiContextPage, context).toOption.value
        val request = DataRequest(FakeRequest(), FakeUser, userAnswers)
        new AddAnApiCheckContextActionProviderImpl().apply(context).invokeBlock(request, invokeBlock).map {
          result =>
            result mustBe Ok
        }
      }
    }

    "must redirect to the journey recovery page when the context is incorrect" in {
      val contexts = Table(
        ("User answers context", "URL context"),
        (AddAnApi, AddEndpoints),
        (AddEndpoints, AddAnApi)
      )

      forAll(contexts) {(userAnswersContext: AddAnApiContext, urlContext: AddAnApiContext) =>
        val userAnswers = UserAnswers("id").set(AddAnApiContextPage, userAnswersContext).toOption.value
        val request = DataRequest(FakeRequest(), FakeUser, userAnswers)
        new AddAnApiCheckContextActionProviderImpl().apply(urlContext).invokeBlock(request, invokeBlock).map {
          result =>
            result mustBe Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
      }
    }

    "must redirect to the journey recovery page when there is no context in user answers" in {
      val contexts = Table(
        "Context",
        AddAnApi,
        AddEndpoints
      )

      forAll(contexts) {(context: AddAnApiContext) =>
        val userAnswers = UserAnswers("id")
        val request = DataRequest(FakeRequest(), FakeUser, userAnswers)
        new AddAnApiCheckContextActionProviderImpl().apply(context).invokeBlock(request, invokeBlock).map {
          result =>
            result mustBe Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
      }
    }
  }

}

object AddAnApiCheckContextActionSpec {

  private val invokeBlock: DataRequest[?] => Future[Result] = _ => Future.successful(Ok)

}
