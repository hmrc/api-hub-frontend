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

package models.myapis.produce

import models.myapis.produce.ProduceApiEgressPrefixes
import play.api.libs.json.*

case class ProduceApiEgressPrefixMapping(existing: String, replacement: String)

object ProduceApiEgressPrefixMapping {
  implicit val format: Format[ProduceApiEgressPrefixMapping] = Json.format[ProduceApiEgressPrefixMapping]
}

case class ProduceApiEgressPrefixes(prefixes: Seq[String], mappings: Seq[String]) {
  import ProduceApiEgressPrefixes.mappingSeparator
  
  def getMappings: Seq[ProduceApiEgressPrefixMapping] = {
    mappings.map { mapping =>
      val split = mapping.split(mappingSeparator)
      ProduceApiEgressPrefixMapping(split(0), split(1))
    }
  }
}

object ProduceApiEgressPrefixes {
  implicit val format: Format[ProduceApiEgressPrefixes] = Json.format[ProduceApiEgressPrefixes]
  
  val mappingSeparator = "->" // see mappingSeparator in buildView() in produceApiEgressPrefixes.js 

  def unapply(egressPrefixes: ProduceApiEgressPrefixes): Option[(Seq[String], Seq[String])] = Some((egressPrefixes.prefixes, egressPrefixes.mappings))
}
