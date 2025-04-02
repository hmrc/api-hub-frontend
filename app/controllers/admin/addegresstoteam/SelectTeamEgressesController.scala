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

package controllers.admin.addegresstoteam

import com.google.inject.{Inject, Singleton}
import config.HipEnvironments
import controllers.actions.{AddEgressToTeamDataRetrievalAction, AuthorisedSupportAction, DataRequiredAction, IdentifierAction}
import controllers.helpers.ErrorResultBuilder
import forms.admin.addegresstoteam.SelectTeamEgressFormProvider
import models.Mode
import navigation.Navigator
import pages.admin.addegresstoteam.{AddEgressToTeamTeamPage, SelectTeamEgressesPage}
import play.api.i18n.I18nSupport
import play.api.mvc.*
import repositories.AddEgressToTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.admin.addegresstoteam.SelectTeamEgressView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectTeamEgressesController @Inject()(
                                              override val controllerComponents: MessagesControllerComponents,
                                              identify: IdentifierAction,
                                              isSupport: AuthorisedSupportAction,
                                              getData: AddEgressToTeamDataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              navigator: Navigator,
                                              errorResultBuilder: ErrorResultBuilder,
                                              apiHubService: ApiHubService,
                                              view: SelectTeamEgressView,
                                              formProvider: SelectTeamEgressFormProvider,
                                              sessionRepository: AddEgressToTeamSessionRepository,
                                              hipEnvironments: HipEnvironments
                                           )(implicit ex: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen isSupport andThen getData andThen requireData).async {
    implicit request => {
      apiHubService.listEgressGateways(hipEnvironments.deployTo).map(egressGateways => {
        request.userAnswers.get(AddEgressToTeamTeamPage) match {
          case None => errorResultBuilder.teamNotFound("unknown")
          case Some(team) =>
            val preparedForm = request.userAnswers.get(SelectTeamEgressesPage) match {
              case None => form
              case Some(values) => form.fill(values)
            }

            val notAlreadyAddedEgressGateways = egressGateways.filterNot { egressGateway =>
              team.egresses.contains(egressGateway.id)
            }.sortBy(_.friendlyName.toLowerCase)

            Ok(view(preparedForm, team, notAlreadyAddedEgressGateways, request.user))
        }
      })
    }
  }
  
  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen isSupport andThen getData andThen requireData).async {
    implicit request => {
      form.bindFromRequest().fold(
        formWithErrors =>
          apiHubService.listEgressGateways(hipEnvironments.deployTo).map(egressGateways => {
            request.userAnswers.get(AddEgressToTeamTeamPage) match {
              case None => errorResultBuilder.teamNotFound("unknown")
              case Some(team) => BadRequest(view(formWithErrors, team, egressGateways, request.user))
            }
          }),
        values =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectTeamEgressesPage, values))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(SelectTeamEgressesPage, mode, updatedAnswers))
      )
    }
  }

}
