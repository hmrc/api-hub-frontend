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

package matchers

import config.HipEnvironment
import org.scalatest.*
import matchers.*

trait CustomMatchers {

  def matchHipEnvironment(hipEnvironment: HipEnvironment) = HipEnvironmentMatcher(hipEnvironment)

  case class HipEnvironmentMatcher(hipEnvironment: HipEnvironment) extends Matcher[HipEnvironment] {
    override def apply(left: HipEnvironment): MatchResult = {
      MatchResult(left.id == hipEnvironment.id &&
        left.rank == hipEnvironment.rank &&
        left.isProductionLike == hipEnvironment.isProductionLike &&
        (
          (left.promoteTo.isEmpty && hipEnvironment.promoteTo.isEmpty) ||
          (left.promoteTo.isDefined && hipEnvironment.promoteTo.isDefined && left.promoteTo.get.id == hipEnvironment.promoteTo.get.id)
        ),"HipEnvironment does not match", "HipEnvironment matches")
    }
  }
}