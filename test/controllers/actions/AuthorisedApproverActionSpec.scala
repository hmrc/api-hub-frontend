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

import controllers.routes
import models.requests.IdentifierRequest
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorisedApproverActionSpec extends AnyFreeSpec with Matchers{

  "AuthorisedApproverAction" - {
    "must return the block's result when the user is an approver" in {
      val request = IdentifierRequest(FakeRequest(), FakeApprover)
      val action = new AuthorisedApproverAction()
      val block = (_: IdentifierRequest[?]) => Future.successful(Results.Ok)

      val result = action.invokeBlock(request, block)
      status(result) mustBe OK
    }

    "must return a redirection to the unauthorised page if the user is not an approver" in {
      val request = IdentifierRequest(FakeRequest(), FakeUser)
      val action = new AuthorisedApproverAction()
      val block = (_: IdentifierRequest[?]) => Future.successful(Results.Ok)

      val result = action.invokeBlock(request, block)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }
  }

}
