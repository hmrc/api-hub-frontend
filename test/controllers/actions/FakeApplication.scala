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
  Creator(FakeUser.email.get),
  LocalDateTime.now(),
  Seq(TeamMember(FakeUser.email.get)),
  Environments()
)

object FakeApplicationWithSecrets extends Application(
  "fake-application-id",
  "fake-application-name",
  LocalDateTime.now(),
  Creator(FakeUser.email.get),
  LocalDateTime.now(),
  Seq(TeamMember(FakeUser.email.get)),
  Environments(primary = Environment(scopes = Seq(Scope("scope_name", Approved)), credentials = Seq(Credential("primary_client_id", None, Some("primary fragment")))),
    secondary = Environment(scopes = Seq(Scope("scope_name", Approved)), credentials = Seq(Credential("secondary_client_id", Some("secondary secret"), Some("secondary fragment")))),
    dev = Environment(),
    test = Environment(),
    preProd = Environment(),
    prod = Environment())
)

object FakeApplicationWithIdButNoSecrets extends Application(
  "fake-application-id",
  "fake-application-name",
  LocalDateTime.now(),
  Creator(FakeUser.email.get),
  LocalDateTime.now(),
  Seq(TeamMember(FakeUser.email.get)),
  Environments(primary = Environment(scopes = Seq(Scope("scope_name", Approved)), credentials = Seq(Credential("primary_client_id", None, None))),
    secondary = Environment(scopes = Seq(Scope("scope_name", Approved)), credentials = Seq(Credential("secondary_client_id", None, None))),
    dev = Environment(),
    test = Environment(),
    preProd = Environment(),
    prod = Environment())
)
