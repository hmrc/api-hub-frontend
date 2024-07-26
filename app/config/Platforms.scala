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
import models.api.Platform
import play.api.Configuration

trait Platforms {
  def platforms: Seq[Platform]

  def getDescription(platformCode: String): String = {
    platforms.find(platform => codesMatch(platform.code, platformCode))
      .map(_.description)
      .getOrElse(platformCode)
  }

  def isSelfServe(platformCode: String): Boolean = {
    platforms.find(platform => codesMatch(platform.code, platformCode))
      .map(_.isSelfServe)
      .getOrElse(false)
  }

  private def normalise(s: String): String = {
    s.trim.toLowerCase()
  }

  protected def codesMatch(platformCode1: String, platformCode2: String): Boolean = {
    normalise(platformCode1) == normalise(platformCode2)
  }

}

@Singleton
class PlatformsImpl @Inject()(configuration: Configuration) extends Platforms {
  private val selfServePlatforms = configuration.get[Seq[String]]("selfServePlatforms").map(_.toLowerCase).toSet

  override val platforms: Seq[Platform] = configuration.get[Map[String,String]]("platforms")
    .map{
      case (code, description) => Platform(
        code,
        description,
        selfServePlatforms.find(selfServePlatformCode => codesMatch(selfServePlatformCode, code)).isDefined)
    }
    .toSeq
    .sortBy(_.description.toLowerCase)

}
