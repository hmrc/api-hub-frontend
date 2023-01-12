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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{Application, CheckMode, UserAnswers}
import pages.ApplicationNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.ApplicationsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

class CreateApplicationController @Inject()(
    override val messagesApi: MessagesApi,
    override val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    applicationsService: ApplicationsService
  )(implicit ec: ExecutionContext)
  extends FrontendBaseController
  with I18nSupport {

  def create(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validateAndBuildApplication(request.userAnswers).fold(
        call => Future.successful(Redirect(call)),
        application => applicationsService.create(application)
          .map(_ => Redirect(routes.IndexController.onPageLoad))
      )
  }

  private def validateAndBuildApplication(userAnswers: UserAnswers): Either[Call, Application] = {
    userAnswers.get(ApplicationNamePage) match {
      case Some(name) => Right(Application(None, name))
      case _ => Left(routes.ApplicationNameController.onPageLoad(CheckMode))
    }
  }

}
