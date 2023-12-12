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

import com.google.inject.{Inject, Singleton}
import controllers.routes
import models.AddAnApiContext
import models.requests.DataRequest
import pages.AddAnApiContextPage
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import scala.concurrent.{ExecutionContext, Future}

trait AddAnApiCheckContextAction extends ActionFilter[DataRequest]

trait AddAnApiCheckContextActionProvider {

  def apply(context: AddAnApiContext)(implicit ec: ExecutionContext): AddAnApiCheckContextAction

}

@Singleton
class AddAnApiCheckContextActionProviderImpl @Inject()() extends AddAnApiCheckContextActionProvider {

  override def apply(context: AddAnApiContext)(implicit ec: ExecutionContext): AddAnApiCheckContextAction = {
    new AddAnApiCheckContextAction() {
      override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
        request.userAnswers.get(AddAnApiContextPage) match {
          case Some(answer) if answer == context => Future.successful(None)
          case _ => Future.successful(Some(Redirect(routes.JourneyRecoveryController.onPageLoad())))
        }
      }

      override protected def executionContext: ExecutionContext = ec
    }
  }

}
