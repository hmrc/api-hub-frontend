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
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.myapis.produce.{ProduceApiEnterApiTitlePage, ProduceApiReviewNameDescriptionPage, ProduceApiShortDescriptionPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.ProduceApiSessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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

  val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiReviewNameDescriptionPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      withJorneyRecovery { case (apiName, apiShortDescription) =>
        Ok (view(preparedForm, apiName, apiShortDescription, request.user))
      }
  }

  def getApiName(userAnswers: UserAnswers): Option[String] = {
    userAnswers.get(ProduceApiEnterApiTitlePage)
  }
  
  def getApiShortDescription(userAnswers: UserAnswers): Option[String] = {
    userAnswers.get(ProduceApiShortDescriptionPage)
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            withJorneyRecovery { case (apiName, apiShortDescription) =>
              BadRequest(view(formWithErrors, apiName, apiShortDescription, request.user))
            }
          ),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ProduceApiReviewNameDescriptionPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiReviewNameDescriptionPage, NormalMode, updatedAnswers))
      )
  }

  private def withJorneyRecovery(f: (String, String) => Result)(implicit request: DataRequest[?]) =
    val apiName = getApiName(request.userAnswers)
    val apiShortDescription = getApiShortDescription(request.userAnswers)
    (apiName, apiShortDescription) match {
      case (Some(name), Some(description)) => f(name, description)
      case _ => Redirect(routes.JourneyRecoveryController.onPageLoad())
    }

}
