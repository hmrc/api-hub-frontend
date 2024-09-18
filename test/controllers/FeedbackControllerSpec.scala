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

package controllers

import base.OptionallyAuthenticatedSpecBase
import controllers.actions.FakeUser
import forms.FeedbackFormProvider
import models.Feedback
import models.Feedback.FeedbackType.*
import org.scalatest.OptionValues
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.HtmlValidation
import views.html.FeedbackView

class FeedbackControllerSpec extends OptionallyAuthenticatedSpecBase with OptionValues with HtmlValidation {


  private val formProvider = new FeedbackFormProvider()
  private val form = formProvider()

  "FeedbackController" - {

    "must return OK and the correct view for a GET with an unauthenticated user" in {
      val application = applicationBuilder(None).build()

      running(application) {
        val request = FakeRequest(GET, routes.FeedbackController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[FeedbackView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, None)(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return OK and the correct view for a GET with an authenticated user" in {
      val application = applicationBuilder(Some(FakeUser)).build()

      running(application) {
        val request = FakeRequest(GET, routes.FeedbackController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[FeedbackView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, Some(FakeUser))(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must return SEE_OTHER on submit with an unauthenticated user" in {
      val application = applicationBuilder(None).build()

      running(application) {
        val feedback = Feedback(
          `type` = Other,
          otherType = Some("otherType"),
          rate = 5,
          comments = "comments",
          allowContact = true,
          email = Some("email@email.com"),
        )
        val feedbackValues: Map[String, String] = Map(
          "type" -> feedback.`type`.toString,
          "otherType" -> feedback.otherType.getOrElse(""),
          "rate" -> feedback.rate.toString,
          "comments" -> feedback.comments,
          "allowContact" -> feedback.allowContact.toString,
          "allowContactEmail" -> feedback.email.getOrElse(""),
        )
        val request = FakeRequest(POST, routes.FeedbackController.onSubmit().url)
          .withFormUrlEncodedBody(feedbackValues.toSeq*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must return SEE_OTHER on submit for a POST with an authenticated user" in {
      val application = applicationBuilder(Some(FakeUser)).build()

      running(application) {
        val feedback = Feedback(
          `type` = Other,
          otherType = Some("otherType"),
          rate = 5,
          comments = "comments",
          allowContact = true,
          email = Some("email@email.com"),
        )
        val feedbackValues: Map[String, String] = Map(
          "type" -> feedback.`type`.toString,
          "otherType" -> feedback.otherType.getOrElse(""),
          "rate" -> feedback.rate.toString,
          "comments" -> feedback.comments,
          "allowContact" -> feedback.allowContact.toString,
          "allowContactEmail" -> feedback.email.getOrElse(""),
        )
        val request = FakeRequest(POST, routes.FeedbackController.onSubmit().url)
          .withFormUrlEncodedBody(feedbackValues.toSeq*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must return BAD_REQUEST on submit for a POST with an invalid form" in {
      val application = applicationBuilder(Some(FakeUser)).build()

      running(application) {
        val feedbackValues: Map[String, String] = Map(
          "bad" -> "value"
        )
        val request = FakeRequest(POST, routes.FeedbackController.onSubmit().url)
          .withFormUrlEncodedBody(feedbackValues.toSeq*)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }
  }
}
