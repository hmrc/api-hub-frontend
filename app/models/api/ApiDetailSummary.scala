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

package models.api

import config.Platforms
import play.api.libs.json.{Format, Json}

case class ApiDetailSummary(
                             id: String,
                             publisherReference: String,
                             title: String,
                             shortDescription: Option[String],
                             apiStatus: ApiStatus,
                             domain: Option[String],
                             subDomain: Option[String],
                             hods: Seq[String],
                             platform: String,
                             apiType: Option[ApiType],
                             teamId: Option[String],
                             apiNumber: Option[String],
                             apiGeneration: Option[ApiGeneration],
                             isSelfServe: Option[Boolean],
                             isEISManaged: Option[Boolean],
)

object ApiDetailSummary {

  def apply(apiDetail: ApiDetail, platforms: Platforms): ApiDetailSummary = {
    ApiDetailSummary(
      id = apiDetail.id,
      publisherReference = apiDetail.publisherReference,
      title = apiDetail.title,
      shortDescription = apiDetail.shortDescription,
      apiStatus = apiDetail.apiStatus,
      domain = apiDetail.domain,
      subDomain = apiDetail.subDomain,
      hods = apiDetail.hods,
      platform = apiDetail.platform,
      apiType = apiDetail.apiType,
      teamId = apiDetail.teamId,
      apiNumber = apiDetail.apiNumber,
      apiGeneration = apiDetail.apiGeneration,
      isSelfServe = Some(apiDetail.isSelfServe(platforms)),
      isEISManaged = Some(apiDetail.isEISManaged(platforms)),
    )
  }

  implicit val formatApiDetailSummary: Format[ApiDetailSummary] = Json.format[ApiDetailSummary]

}
