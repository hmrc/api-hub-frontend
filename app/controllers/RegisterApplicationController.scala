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
import models.{CheckMode, UserAnswers}
import models.application.{Creator, NewApplication}
import pages.ApplicationNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

class RegisterApplicationController @Inject()(
    override val messagesApi: MessagesApi,
    override val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    apiHubService: ApiHubService
  )(implicit ec: ExecutionContext)
  extends FrontendBaseController
  with I18nSupport {

  def create(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validateAndBuildApplication(request.userAnswers).fold(
        call => Future.successful(Redirect(call)),
        newApplication => apiHubService.registerApplication(newApplication)
          .map(app => Redirect(routes.RegisterApplicationSuccessController.onPageLoad(app.id)))
      )
  }

  private def validateAndBuildApplication(userAnswers: UserAnswers): Either[Call, NewApplication] = {
    for {
      applicationName <- validateApplicationName(userAnswers)
    } yield NewApplication(applicationName, Creator(email = ""))
  }

  private def validateApplicationName(userAnswers: UserAnswers): Either[Call, String] = {
    userAnswers.get(ApplicationNamePage) match {
      case Some(applicationName) => Right(applicationName)
      case _ => Left(routes.ApplicationNameController.onPageLoad(CheckMode))
    }
  }

}
