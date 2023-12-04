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

package utils

import controllers.actions.{FakeApprover, FakePrivilegedUser, FakeSupporter, FakeUser}
import models.user.UserModel
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}

trait TestHelpers extends TableDrivenPropertyChecks {

  val usersWhoCanViewApprovals: TableFor1[UserModel] = Table(
    "User",
    FakeApprover,
    FakeSupporter
  )

  val usersWhoCannotViewApprovals: TableFor1[UserModel] = Table(
    "User",
    FakeUser,
    FakePrivilegedUser
  )

  val usersWhoCannotApprove: TableFor1[UserModel] = Table(
    "User",
    FakeUser,
    FakePrivilegedUser,
    FakeSupporter
  )

  val teamMemberAndSupporterTable: TableFor1[UserModel] = Table(
    "User",
    FakeUser,
    FakeSupporter
  )

  val privilegedTeamMemberAndSupporterTable: TableFor1[UserModel] = Table(
    "User",
    FakeUser.copy(permissions = FakeUser.permissions.copy(isPrivileged = true)),
    FakeSupporter.copy(permissions = FakeSupporter.permissions.copy(isPrivileged = true))
  )

  val usersWhoCanDeleteSecondaryCredentials: TableFor1[UserModel] = Table(
    "User",
    FakeUser,
    FakePrivilegedUser,
    FakeSupporter
  )

  val usersWhoCannotDeleteSecondaryCredentials: TableFor1[UserModel] = Table(
    "User",
    FakeApprover
  )

  val usersWhoCanDeletePrimaryCredentials: TableFor1[UserModel] = Table(
    "User",
    FakePrivilegedUser,
    FakeSupporter
  )

  val usersWhoCannotDeletePrimaryCredentials: TableFor1[UserModel] = Table(
    "User",
    FakeUser,
    FakeApprover
  )

}
