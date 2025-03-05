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

package forms.application.accessrequest

import forms.application.accessrequest.RequestProductionAccessSelectApisFormProvider
import forms.behaviours.CheckboxFieldBehaviours
import play.api.data.FormError
import viewmodels.application.ApplicationApi

class RequestProductionAccessSelectApisFormProviderSpec extends CheckboxFieldBehaviours {

  private val applicationApi =
    ApplicationApi(
      apiId = "api-id-1",
      apiTitle = "API title 1",
      endpoints = Seq.empty,
      pendingAccessRequests = Seq.empty,
      isMissing = false
    )
  private val applicationApi2 =
    ApplicationApi(
      apiId = "api-id-2",
      apiTitle = "API title 2",
      endpoints = Seq.empty,
      pendingAccessRequests = Seq.empty,
      isMissing = false
    )
  private val applicationApi3 =
    ApplicationApi(
      apiId = "api-id-3",
      apiTitle = "API title 3",
      endpoints = Seq.empty,
      pendingAccessRequests = Seq.empty,
      isMissing = false
    )
  private val applicationApis = Seq(
    applicationApi,
    applicationApi2,
    applicationApi3,
  )
  private val applicationApiIds = applicationApis.map(_.apiId)
  private val form = new RequestProductionAccessSelectApisFormProvider()(applicationApis.toSet)


  ".value" - {

    val fieldName = "value"
    val requiredKey = "requestProductionAccessSelectApis.error.required"

    behave like checkboxField[String](
      form,
      fieldName,
      validValues  = applicationApiIds,
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
