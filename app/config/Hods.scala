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
import models.api.Hod
import play.api.Configuration

trait Hods {
  def hods: Seq[Hod]

  def getDescription(hodCode: String): String = {
    hods.find(hod => normalise(hod.code).equals(normalise(hodCode)))
      .map(_.description)
      .getOrElse(hodCode)
  }

  private def normalise(s: String): String = {
    s.trim.toLowerCase()
  }

}

@Singleton
class HodsImpl @Inject()(configuration: Configuration) extends Hods {

  override val hods: Seq[Hod] = configuration.get[Map[String,String]]("hods")
    .map{ case (name, description) => Hod(name, description) }
    .toSeq
    .sortBy(_.description.toLowerCase)

}
