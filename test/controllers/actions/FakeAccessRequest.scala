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

package controllers.actions

import fakes.FakeHipEnvironments
import models.accessrequest.{AccessRequest, AccessRequestDecision, AccessRequestEndpoint, Approved}

import java.time.LocalDateTime

object FakeAccessRequest extends AccessRequest(
  "access-request-id",
  "application-id",
  "api-id",
  "api-name",
  Approved,
  Seq(
    AccessRequestEndpoint(
      "GET",
      "/test1",
      Seq("test-scope")
    )
  ),
  "supportingInformation",
  LocalDateTime.now(),
  "requestedBy",
  Some(AccessRequestDecision(LocalDateTime.now(), "decider", None)),
  None,
  FakeHipEnvironments.production.id
)
