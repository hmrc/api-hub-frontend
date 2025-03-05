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

package fakes

import config.{BaseHipEnvironment, DefaultHipEnvironment, HipEnvironment, HipEnvironments}

object FakeHipEnvironments extends HipEnvironments {

  val production: HipEnvironment = DefaultHipEnvironment(
    id = "production",
    rank = 1,
    isProductionLike = true,
    promoteTo = None
  )

  val preProduction: HipEnvironment = DefaultHipEnvironment(
    id = "preprod",
    rank = 2,
    isProductionLike = true,
    promoteTo = Some(production)
  )

  val test: HipEnvironment = DefaultHipEnvironment(
    id = "test",
    rank = 3,
    isProductionLike = false,
    promoteTo = Some(preProduction)
  )

  override def environments: Seq[HipEnvironment] = Seq(production, preProduction, test)

  override protected def baseEnvironments: Seq[BaseHipEnvironment] = Seq.empty

  override def deployTo = test
}