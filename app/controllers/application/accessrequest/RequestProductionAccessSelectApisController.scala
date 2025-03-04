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

package controllers.application.accessrequest

import config.{HipEnvironment, HipEnvironments}
import controllers.actions.*
import controllers.application.accessrequest.RequestProductionAccessEndJourneyController.Data
import controllers.routes
import forms.application.accessrequest.RequestProductionAccessSelectApisFormProvider
import models.{Mode, UserAnswers}
import models.requests.DataRequest
import navigation.Navigator
import pages.application.accessrequest.{RequestProductionAccessApisPage, RequestProductionAccessEnvironmentIdPage, RequestProductionAccessSelectApisPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.AccessRequestSessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.application.ApplicationApi
import views.html.application.accessrequest.RequestProductionAccessSelectApisView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RequestProductionAccessSelectApisController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: AccessRequestSessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: AccessRequestDataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: RequestProductionAccessSelectApisFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: RequestProductionAccessSelectApisView,
                                        hipEnvironments: HipEnvironments
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport:

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      validateEnvironmentId(request.userAnswers).fold(
        call => Future.successful(Redirect(call)),
        hipEnvironment => viewWithApiApplications(
          mode,
          (applicationApis: Seq[ApplicationApi], applicationApisPendingRequest: Seq[ApplicationApi]) =>
            Future.successful(Ok(view(preparedForm(applicationApis), mode, applicationApis, applicationApisPendingRequest, request.user, hipEnvironment)))
        )
      )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      validateEnvironmentId(request.userAnswers).fold(
        call => Future.successful(Redirect(call)),
        hipEnvironment => viewWithApiApplications(
          mode,
          (applicationApis: Seq[ApplicationApi], applicationApisPendingRequest: Seq[ApplicationApi]) =>
            formProvider(applicationApis.toSet).bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, mode, applicationApis, applicationApisPendingRequest, request.user, hipEnvironment))),

              selectedApiIds =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(RequestProductionAccessSelectApisPage, selectedApiIds))
                  _ <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(RequestProductionAccessSelectApisPage, mode, updatedAnswers))
            )
        )
      )
  }

  private def partitionApplicationApis(applicationApis: Seq[ApplicationApi]): (Seq[ApplicationApi], Seq[ApplicationApi]) =
    val (applicationApisPendingRequest, applicationApisNoPendingRequest) = applicationApis.partition(_.hasPendingAccessRequest)
    val applicationsThatCanRequestAccess = applicationApisNoPendingRequest.filter(api => !api.isMissing && api.needsProductionAccessRequest)
    (applicationsThatCanRequestAccess, applicationApisPendingRequest)

  private def preparedForm(applicationApis: Seq[ApplicationApi])(implicit request: DataRequest[?]) =
    val form = formProvider(applicationApis.toSet)
    request.userAnswers.get(RequestProductionAccessSelectApisPage) match {
      case None => form
      case Some(value) => form.fill(value)
    }

  private def viewWithApiApplications(
                                       mode: Mode,
                                       result: (
                                         applicationApis: Seq[ApplicationApi],
                                         applicationApisPendingRequests: Seq[ApplicationApi]
                                       ) => Future[Result]
                                     )(implicit hc: HeaderCarrier, request: DataRequest[?]): Future[Result] =
    request.userAnswers.get(RequestProductionAccessApisPage) match {
      case Some(apis) =>
        result.tupled(partitionApplicationApis(apis))
      case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  private def validateEnvironmentId(userAnswers: UserAnswers): Either[Call, HipEnvironment] = {
    userAnswers.get(RequestProductionAccessEnvironmentIdPage) match {
      case Some(environmentId) => Right(hipEnvironments.forId(environmentId))
      case None => Left(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }