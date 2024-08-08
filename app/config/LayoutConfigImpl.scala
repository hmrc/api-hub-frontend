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

package config

import com.google.inject.{Inject, Singleton}
import controllers.routes
import models.api.Platform
import play.api.Configuration
import uk.gov.hmrc.integrationhub.LayoutConfig


@Singleton
class LayoutConfigImpl @Inject()(appConfig: FrontendAppConfig) extends LayoutConfig(
  appConfig.timeout,
  appConfig.countdown,
  routes.KeepAliveController.keepAlive.url,
  controllers.auth.routes.AuthController.signOut().url,
  appConfig.helpDocsPath,
  "/api-hub" + routes.ServiceStartController.onPageLoad.url,
  "/api-hub" + routes.IndexController.onPageLoad.url,
  "/api-hub/admin" + controllers.admin.routes.AccessRequestsController.onPageLoad().url,
  "/api-hub" + routes.ExploreApisController.onPageLoad().url,
  "/api-hub" + routes.GetSupportController.onPageLoad.url,
  appConfig.helpDocsPath
) {}
