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

package models.user

import viewmodels.WithName

sealed trait UserType

case object LdapUser extends WithName("LDAP") with UserType
case object StrideUser extends WithName("Stride") with UserType

case class Permissions(canApprove: Boolean, canSupport: Boolean, isPrivileged: Boolean)

object Permissions {

  def apply(): Permissions = Permissions(canApprove = false, canSupport = false, isPrivileged = false)

}

case class UserModel(
  userId: String,
  userType: UserType,
  email: String,
  permissions: Permissions = Permissions()
)
