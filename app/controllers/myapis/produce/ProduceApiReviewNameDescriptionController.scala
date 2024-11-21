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

package controllers.myapis.produce

import controllers.actions.*
import controllers.routes
import forms.myapis.produce.ProduceApiReviewNameDescriptionFormProvider
import models.requests.DataRequest
import models.{Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.myapis.produce.{ProduceApiEnterApiTitlePage, ProduceApiReviewNameDescriptionPage, ProduceApiShortDescriptionPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.myapis.produce.ProduceApiReviewNameDescriptionViewModel
import views.html.myapis.produce.ProduceApiReviewNameDescriptionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiReviewNameDescriptionController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: ProduceApiSessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: ProduceApiDataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: ProduceApiReviewNameDescriptionFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ProduceApiReviewNameDescriptionView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  private def viewModel(mode: Mode) =
    ProduceApiReviewNameDescriptionViewModel(
      controllers.myapis.produce.routes.ProduceApiReviewNameDescriptionController.onSubmit(mode),
      controllers.myapis.produce.routes.ProduceApiShortDescriptionController.onPageLoad(mode).url,
      controllers.myapis.produce.routes.ProduceApiEnterOasController.onPageLoad(mode).url
    )

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      withJourneyRecovery { case (apiName, apiShortDescription) =>
        Ok(view(form, mode, apiName, apiShortDescription, request.user, viewModel(mode))) // never pre-fill checkbox, even in CheckMode
      }
  }

  def getApiName(userAnswers: UserAnswers): Option[String] = {
    userAnswers.get(ProduceApiEnterApiTitlePage)
  }
  
  def getApiShortDescription(userAnswers: UserAnswers): Option[String] = {
    userAnswers.get(ProduceApiShortDescriptionPage)
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            withJourneyRecovery { case (apiName, apiShortDescription) =>
              BadRequest(view(formWithErrors, mode, apiName, apiShortDescription, request.user, viewModel(mode)))
            }
          ),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiReviewNameDescriptionPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiReviewNameDescriptionPage, mode, updatedAnswers))
      )
  }

  private def withJourneyRecovery(f: (String, String) => Result)(implicit request: DataRequest[?]) =
    val apiName = getApiName(request.userAnswers)
    val apiShortDescription = getApiShortDescription(request.userAnswers)
    (apiName, apiShortDescription) match {
      case (Some(name), Some(description)) => f(name, description)
      case _ => Redirect(routes.JourneyRecoveryController.onPageLoad())
    }

}
