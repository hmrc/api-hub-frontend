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

package forms.application

import forms.mappings.Mappings
import models.{AvailableEndpoints, Enumerable}
import play.api.data.Form
import play.api.data.Forms.set
import viewmodels.application.ApplicationApi

import javax.inject.Inject

class RequestProductionAccessSelectApisFormProvider @Inject() extends Mappings {

  def apply(applicationAPIs: Set[ApplicationApi]): Form[Set[String]] =
    val validApplicationAPIs = applicationAPIs.filterNot(_.isMissing)

    given enumerableScopes: Enumerable[String] = (str: String) => {
      validApplicationAPIs.collectFirst { case api if api.apiId == str => api.apiId }
    }

    Form(
      "value" -> set(enumerable[String]("requestProductionAccessSelectApis.error.required"))
        .verifying(nonEmptySet("requestProductionAccessSelectApis.error.required"))
    )
}
