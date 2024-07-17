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

package controllers.actions

import models.api.{ApiDetail, Endpoint, EndpointMethod, Live}

import java.time.Instant


object FakeApiDetail extends ApiDetail(
  "apiId",
  "pubRef",
  "title",
  "description",
  "version",
  Seq(Endpoint("/path", Seq(EndpointMethod("GET", None, None, Seq.empty)))),
  Some("short description"),
  "oas",
  Live,
  Some("teamId"),
  Some("domain"),
  Some("subdomain"),
  Seq("hod1"),
  Instant.now()
)
