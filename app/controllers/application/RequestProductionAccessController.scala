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
import viewmodels.application.Inaccessible
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
    implicit request => {
      request.userAnswers.get(AccessRequestApplicationIdPage) match {
        case Some(_) =>
          val previousAnswers = request.userAnswers.get(RequestProductionAccessPage)
          previousAnswers match {
            case None => showPage(form, OK)
            case Some(value) => showPage(form.fill(value), OK)
          }

        case None => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }


  private def showPage(form: Form[_], status: Int)(implicit request: DataRequest[AnyContent]) = {
    request.userAnswers.get(AccessRequestApplicationIdPage) match {
      case Some(application) =>
        applicationApiBuilder.build(application)
          .map {
            case Right(applicationApis) =>
              val filteredApis = applicationApis.filter(_.endpoints.exists(_.primaryAccess == Inaccessible))
                .map(applicationApi => {
                  val filteredEndpoints = applicationApi.endpoints.filter(_.primaryAccess == Inaccessible)
                  val prunedApi = applicationApi.copy(endpoints = filteredEndpoints)
                  prunedApi
                })

              Status(status)(requestProductionAccessView(
                form,
                application, filteredApis, Some(request.user)))
            case Left(result) => result
          }

      case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => showPage(formWithErrors, BAD_REQUEST),
        value => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(RequestProductionAccessPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(controllers.application.routes.ProvideSupportingInformationController.onPageLoad())
        })

  }
}
