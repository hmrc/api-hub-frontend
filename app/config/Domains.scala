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
import models.api.{Domain, SubDomain}
import play.api.Configuration

import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class Domains @Inject()(configuration: Configuration) {
  val domains: Seq[Domain] = configuration.get[Seq[Configuration]]("domains").map {
    configuration => {
      val config = configuration.underlying
      Domain(
        code = config.getString("code"),
        description = config.getString("description"),
        subDomains = config.getConfig("subdomains").entrySet.asScala.map {
          case e => SubDomain(e.getKey, e.getValue.unwrapped().toString)
        }.toSeq
      )
    }
  }
}
