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

package controllers.myapis.update

import config.FrontendAppConfig
import controllers.actions.*
import models.NormalMode
import navigation.Navigator
import pages.myapis.update.UpdateApiBeforeYouStartPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.RelatedContentLink
import viewmodels.myapis.produce.ProduceApiBeforeYouStartViewModel
import views.html.myapis.produce.ProduceApiBeforeYouStartView

import javax.inject.Inject

class UpdateApiBeforeYouStartController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: UpdateApiDataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ProduceApiBeforeYouStartView,
  navigator: Navigator,
  config: FrontendAppConfig
) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val nextPageUrl = navigator.nextPage(UpdateApiBeforeYouStartPage, NormalMode, request.userAnswers).url
      val viewModel = ProduceApiBeforeYouStartViewModel(
        nextPageUrl,
        buildRelatedContentLinks(),
        "updateApiBeforeYouStart.heading",
        "updateApiBeforeYouStart.beforeYouStart.content",
        "updateApiBeforeYouStart.updateProcess.heading",
        "updateApiBeforeYouStart.updateProcess.content",
        Seq.range(1,5).map(i => s"updateApiBeforeYouStart.creationProcess.list.$i")
      )
      Ok(view(request.user, viewModel))
  }

  private def buildRelatedContentLinks()(implicit messages: Messages): Seq[RelatedContentLink] = {
    Seq(
      RelatedContentLink.apiHubGuideLink(
        config,
        messages("produceApiBeforeYouStart.relatedContent.producingApis"),
        "documentation/how-do-i-produce.apis.html#how-do-i-produce-apis"
      ),
      RelatedContentLink.apiHubGuideLink(
        config,
        messages("produceApiBeforeYouStart.relatedContent.consumingApis"),
        "documentation/how-do-I-consume-apis.html#how-do-i-consume-apis"
      )
    )
  }

}
