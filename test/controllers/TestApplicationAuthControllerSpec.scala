package controllers

import base.SpecBase
import controllers.actions.FakeApplication
import play.api.test.FakeRequest
import play.api.test.Helpers._

class TestApplicationAuthControllerSpec extends SpecBase {

  "TestApplicationAuthController" - {
    "should be easy to test" in {
      val application = applicationBuilder().build()
      running(application) {
        val request = FakeRequest(GET, routes.TestApplicationAuthController.onPageLoad(FakeApplication.id).url)
        val result = route(application, request).value

        status(result) mustBe OK
      }
    }
  }

}
