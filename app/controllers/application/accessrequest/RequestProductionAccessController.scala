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

package controllers.application.accessrequest

import com.google.inject.Inject
import config.{HipEnvironment, HipEnvironments}
import controllers.actions.{AccessRequestDataRetrievalAction, DataRequiredAction, IdentifierAction}
import forms.application.accessrequest.RequestProductionAccessDeclarationFormProvider
import models.requests.DataRequest
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.application.accessrequest.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.AccessRequestSessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.Value
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.application.accessrequest.{ProvideSupportingInformationSummary, RequestProductionAccessApplicationSummary, RequestProductionAccessEnvironmentSummary, RequestProductionAccessSelectApisSummary}
import views.html.application.accessrequest.RequestProductionAccessView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left

class RequestProductionAccessController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  requestProductionAccessView: RequestProductionAccessView,
  formProvider: RequestProductionAccessDeclarationFormProvider,
  sessionRepository: AccessRequestSessionRepository,
  getData: AccessRequestDataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: Navigator,
  environmentSummary: RequestProductionAccessEnvironmentSummary,
  hipEnvironments: HipEnvironments
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validateEnvironmentId(request.userAnswers).fold(
        call => Future.successful(Redirect(call)),
        hipEnvironment => {
          val filledForm = request.userAnswers.get(RequestProductionAccessPage) match {
            case Some(value) => form.fill(value)
            case None => form
          }

          Future.successful(showPage(filledForm, OK, hipEnvironment))
        })
  }

  private def validateEnvironmentId(userAnswers: UserAnswers): Either[Call, HipEnvironment] = {
    userAnswers.get(RequestProductionAccessEnvironmentIdPage) match {
      case Some(environmentId) => Right(hipEnvironments.forId(environmentId))
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def showPage(form: Form[?], status: Int, hipEnvironment: HipEnvironment)(implicit request: DataRequest[?]): Result = {
    Status(status)(requestProductionAccessView(
        form,
        buildSummaries(request.userAnswers),
        RequestProductionAccessSelectApisSummary.buildSelectedApis(request.userAnswers),
        Some(request.user),
        hipEnvironment
    ))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validateEnvironmentId(request.userAnswers).fold(
        call => Future.successful(Redirect(call)),
        hipEnvironment => {
          form.bindFromRequest().fold(
            formWithErrors => Future.successful(showPage(formWithErrors, BAD_REQUEST, hipEnvironment)),
            value => {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(RequestProductionAccessPage, value))
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(RequestProductionAccessPage, NormalMode, request.userAnswers).url)
            }
          )
        })
  }

  private def buildSummaries(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(
      RequestProductionAccessApplicationSummary.row(userAnswers),
      environmentSummary.row(userAnswers),
      RequestProductionAccessSelectApisSummary.row(userAnswers),
      ProvideSupportingInformationSummary.row(userAnswers)
    )
  }

}
