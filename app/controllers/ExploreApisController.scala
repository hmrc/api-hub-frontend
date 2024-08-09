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

import com.google.inject.{Inject, Singleton}
import config.{Domains, Hods, Platforms}
import controllers.actions.OptionalIdentifierAction
import models.api.{Alpha, ApiDetail, Beta, Live, Deprecated}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ApiHubService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ExploreApisView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExploreApisController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  apiHubService: ApiHubService,
  view: ExploreApisView,
  optionallyIdentified: OptionalIdentifierAction,
  domains: Domains,
  hods: Hods,
  platforms: Platforms
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = optionallyIdentified.async {
    implicit request =>
      apiHubService.getApis().map {
        case apiDetails: Seq[ApiDetail] => Ok(view(
          request.user,
          apiDetails.sortWith( _.title.toUpperCase < _.title.toUpperCase).zipWithIndex.map { case(a,i) => updateApiDetail(a)},
          domains, hods, platforms
        ))
        case _ => InternalServerError
      }
  }

  def onSubmit() : Action[AnyContent] = optionallyIdentified.async {
    Future.successful(NotImplemented)
  }

  val statuses = Seq(Alpha, Beta, Live, Deprecated)
  val dms = Seq(
    ("1", "1.15"),
    ("1", "1.10"),
    ("1", "1.3"),
    ("2", "2.2"),
    ("3", ""),
    ("", "HMRC"),
    ("invalid", "invalid"),
    ("invalid", ""),
    ("", "invalid"),
    ("", ""),
  )
  val hodsList = Seq("invalid", "ADR", "APIM", "BARS", "CBI", "CBS", "CDCS", "CISR", "COTAX", "GFORMS", "VDP", "DTR", "EDH", "EMS", "ETMP", "IDMS", "ISA", "ITMP", "ITSD", "NPS", "NTC", "ODS", "RCM", "RTI", "SLS", "TPSS")
  val platformsList = Seq("HIP", "API_PLATFORM", "CDS_CLASSIC", "DIGI", "HIP", "DIGI", "HIP", "DAPI")

  def updateApiDetail(apiDetail: ApiDetail): ApiDetail = {
    val i = apiDetail.title.hashCode.abs
    val (dm, sdm) = dms(i % dms.size)
    val hods = hodsList.slice(i % hodsList.size, i % hodsList.size + (i % 3))
    apiDetail.copy(
      apiStatus = statuses(i % statuses.length),
      domain =  if (dm.isEmpty) None else Some(dm),
      subDomain =  if (sdm.isEmpty) None else Some(sdm),
      hods = hods,
      platform = platformsList(i % platformsList.length)
    )
  }
}
