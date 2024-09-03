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

import models.application._

import java.time.LocalDateTime

object FakeApplication extends Application(
  "fake-application-id",
  "fake-application-name",
  LocalDateTime.now(),
  Creator(FakeUser.email),
  LocalDateTime.now(),
  None,
  Seq(TeamMember(FakeUser.email)),
  Environments(),
  Seq.empty
)

object FakeApplicationWithSecrets extends Application(
  "fake-application-id",
  "fake-application-name",
  LocalDateTime.now(),
  Creator(FakeUser.email),
  LocalDateTime.now(),
  None,
  Seq(TeamMember(FakeUser.email)),
  Environments(
    primary = Environment(scopes = Seq(Scope("scope_name")), credentials = Seq(Credential("primary_client_id", LocalDateTime.now(), None, Some("primary fragment")))),
    secondary = Environment(scopes = Seq(Scope("scope_name")), credentials = Seq(Credential("secondary_client_id", LocalDateTime.now(), Some("secondary secret"), Some("secondary fragment"))))
  ),
  Seq.empty
)

object FakeApplicationWithIdButNoSecrets extends Application(
  "fake-application-id",
  "fake-application-name",
  LocalDateTime.now(),
  Creator(FakeUser.email),
  LocalDateTime.now(),
  None,
  Seq(TeamMember(FakeUser.email)),
  Environments(
    primary = Environment(scopes = Seq(Scope("scope_name")), credentials = Seq(Credential("primary_client_id", LocalDateTime.now(), None, None))),
    secondary = Environment(scopes = Seq(Scope("scope_name")), credentials = Seq(Credential("secondary_client_id", LocalDateTime.now(), None, None)))
  ),
  Seq.empty
)
