/*
 * Copyright 2024 HM Revenue & Customs
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
import models.application.Application
import models.requests.{ApplicationRequest, IdentifierRequest}
import play.api.mvc.Result
import play.api.mvc.Results.{NotFound, Redirect}

import scala.concurrent.{ExecutionContext, Future}

trait FakeApplicationAuthActions {

  def successfulApplicationAuthAction(application: Application)(implicit ec: ExecutionContext): ApplicationAuthAction = {
    new ApplicationAuthAction {
      override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, ApplicationRequest[A]]] = {
        Future.successful(Right(ApplicationRequest(request, application)))
      }

      override protected def executionContext: ExecutionContext = implicitly
    }
  }

  def notFoundApplicationAuthAction()(implicit ec: ExecutionContext): ApplicationAuthAction = {
    new ApplicationAuthAction {
      override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, ApplicationRequest[A]]] = {
        Future.successful(Left(NotFound))
      }

      override protected def executionContext: ExecutionContext = implicitly
    }
  }

  def unauthorisedApplicationAuthAction()(implicit ec: ExecutionContext): ApplicationAuthAction = {
    new ApplicationAuthAction {
      override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, ApplicationRequest[A]]] = {
        Future.successful(Left(Redirect(routes.UnauthorisedController.onPageLoad)))
      }

      override protected def executionContext: ExecutionContext = implicitly
    }
  }

}
