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

import models.user.{LdapUser, Permissions, UserModel}

object FakeAdministrator extends UserModel(
  userId ="fake-administrator-id",
  userName = "fake-administrator-name",
  userType = LdapUser,
  email = Some("fake-administrator-email"),
  permissions = Permissions(canApprove = false, canAdminister = true)
)