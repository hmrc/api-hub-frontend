/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.admin

import models.api.ApiDeploymentStatus
import models.api.ApiDeploymentStatus.*
import models.user.UserModel
import play.api.data.Form
import play.api.i18n.Messages

case class ForcePublishViewModel(
  form: Form[?],
  user: UserModel,
  publisherReference: Option[String] = None,
  deploymentStatus: Option[ApiDeploymentStatus] = None,
  catalogueVersion: Option[String] = None
) {

  def hasVersionComparison: Boolean = publisherReference.isDefined

  def deployedVersionText(implicit messages: Messages): String = {
    deploymentStatus match {
      case Some(Deployed(_, version)) => version
      case Some(NotDeployed(_)) => messages("forcePublish.versionComparison.version.notDeployed")
      case _ => messages("forcePublish.versionComparison.version.unknown")
    }
  }

  def catalogueVersionText(implicit messages: Messages): String = {
    catalogueVersion.getOrElse(messages("forcePublish.versionComparison.version.notPublished"))
  }

  def canForcePublish: Boolean = {
    deploymentStatus match {
      case Some(Deployed(_, _)) => true
      case _ => false
    }
  }

}
