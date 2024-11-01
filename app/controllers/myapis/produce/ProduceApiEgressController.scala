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

import config.FrontendAppConfig
import controllers.actions.*
import forms.myapis.produce.ProduceApiChooseEgressFormProvider
import models.{Mode, UserAnswers}
import models.myapis.produce.ProduceApiChooseEgress
import models.requests.DataRequest
import navigation.Navigator
import pages.myapis.produce.{ProduceApiChooseEgressPage, ProduceApiEgressPrefixesPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProduceApiSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.myapis.produce.ProduceApiEgressView
import scala.util.{Try,Success}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProduceApiEgressController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: ProduceApiDataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: ProduceApiEgressView,
                                            config: FrontendAppConfig,
                                            formProvider: ProduceApiChooseEgressFormProvider,
                                            produceApiSessionRepository: ProduceApiSessionRepository,
                                            apiHubService: ApiHubService,
                                            navigator: Navigator
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val preparedForm = request.userAnswers.get(ProduceApiChooseEgressPage) match {
        case None => form
        case Some(egressChoices) => form.fill(egressChoices)
      }

      buildView(mode, preparedForm, Ok)

  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      form.bindFromRequest().fold(
        formWithErrors => buildView(mode, formWithErrors, BadRequest),

        egressChoices =>
          for {
            answersWithEgressChoices <- Future.fromTry(request.userAnswers.set(ProduceApiChooseEgressPage, egressChoices))
            updatedAnswers <- Future.fromTry(removeEgressPrefixAnswersIfNecessary(answersWithEgressChoices)) 
            _ <- produceApiSessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ProduceApiChooseEgressPage, mode, updatedAnswers))
      )
    }
  }
  
  private def removeEgressPrefixAnswersIfNecessary(answers: UserAnswers): Try[UserAnswers] = {
    val willConfigureEgressPrefixes = answers.get(ProduceApiChooseEgressPage).exists(_.configureEgressPrefixes)
    if (willConfigureEgressPrefixes) {
      Success(answers)
    } else {
      answers.remove(ProduceApiEgressPrefixesPage)
    }
  }

  private def buildView(mode: Mode, form: Form[ProduceApiChooseEgress], status: Status)(implicit request: DataRequest[AnyContent]) = {
    apiHubService.listEgressGateways().map(
      egressGateways =>
        status(view(form, mode, request.user, config.helpDocsPath, egressGateways))
    )
  }

}
