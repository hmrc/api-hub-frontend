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

package viewmodels

import com.typesafe.config.Config
import config.FrontendAppConfig
import play.api.{ConfigLoader, Configuration}

import scala.jdk.CollectionConverters._

case class RelatedContentLink(description: String, url: String)

object RelatedContentLink {

  def apiHubGuideLink(config: FrontendAppConfig, description: String, relativeUrl: String): RelatedContentLink = {
    RelatedContentLink(description, s"${config.helpDocsPath}/$relativeUrl")
  }

  implicit val relatedContentLinkConfigLoader: ConfigLoader[Seq[RelatedContentLink]] = ConfigLoader {
    config =>
      path =>
        config.getConfigList(path).asScala.toSeq.map(
          config =>
            RelatedContentLink(
              description = config.getString("description"),
              url = config.getString("url")
            )
        )
  }

}
