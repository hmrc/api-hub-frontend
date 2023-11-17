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

package controllers.application

import com.google.inject.Inject
import controllers.actions.{AccessRequestDataRetrievalAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ApplicationApiBuilder
import controllers.routes
import forms.RequestProductionAccessDeclarationFormProvider
import models.requests.DataRequest
import pages.{AccessRequestApplicationIdPage, RequestProductionAccessPage}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.AccessRequestSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.application.RequestProductionAccessView

import scala.concurrent.{ExecutionContext, Future}

class RequestProductionAccessController @Inject()(
                                                   val controllerComponents: MessagesControllerComponents,
                                                   identify: IdentifierAction,
                                                   requestProductionAccessView: RequestProductionAccessView,
                                                   applicationApiBuilder: ApplicationApiBuilder,
                                                   formProvider: RequestProductionAccessDeclarationFormProvider,
                                                   sessionRepository: AccessRequestSessionRepository,
                                                   getData: AccessRequestDataRetrievalAction,
                                                   requireData: DataRequiredAction)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(RequestProductionAccessPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      showPage(preparedForm)
  }


  private def showPage(form: Form[_])(implicit request: DataRequest[AnyContent]) = {
    request.userAnswers.get(AccessRequestApplicationIdPage) match {
      case Some(application) =>
        applicationApiBuilder.build(application).map {
          case Right(applicationApis) => Ok(requestProductionAccessView(form, application, applicationApis, Some(request.user)))
          case Left(result) => result
        }

      case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
          formWithErrors => {
            Console.println(s"formWithErrors: $formWithErrors")
            showPage(formWithErrors)
          },
          value => {
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(RequestProductionAccessPage, value))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(controllers.application.routes.ProvideSupportingInformationController.onPageLoad())
          })

  }
}
