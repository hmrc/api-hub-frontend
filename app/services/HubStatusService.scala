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

package services

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig

trait HubStatusService {

  def isAvailable: Boolean

  def isShuttered: Boolean

  def shutterMessage: String

  def shutterDown(shutterMessage: String): Unit

  def shutterUp(): Unit

}

@Singleton
class HubStatusServiceImpl @Inject(config: FrontendAppConfig) extends HubStatusService {

  // Needs to be replaced by a repository
  private var shuttered = config.shuttered
  private var message = config.shutterMessage

  override def isAvailable: Boolean = !isShuttered

  override def isShuttered: Boolean = shuttered

  override def shutterMessage: String = message

  override def shutterDown(shutterMessage: String): Unit = {
    shuttered = true
    message = shutterMessage
  }

  override def shutterUp(): Unit = {
    shuttered = false
    message = config.shutterMessage
  }

}
