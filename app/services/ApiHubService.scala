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

package services

import com.google.inject.{Inject, Singleton}
import connectors.ApplicationsConnector
import models.application.{Application, NewApplication}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ApiHubService @Inject()(applicationsConnector: ApplicationsConnector)
  extends Logging {

  def createApplication(application: NewApplication)(implicit hc: HeaderCarrier): Future[Application] = {
    logger.debug(s"Creating application named ${application.name}")
    applicationsConnector.createApplication(application)
  }

  def getApplications()(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    applicationsConnector.getApplications()
  }

}
