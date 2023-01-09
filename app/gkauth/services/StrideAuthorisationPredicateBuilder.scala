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

package gkauth.services

import gkauth.config.StrideAuthRoles
import gkauth.domain.models.GatekeeperRoles.{ADMIN, SUPERUSER, USER}
import gkauth.domain.models.{GatekeeperRoles, GatekeeperStrideRole}
import gkauth.domain.models.GatekeeperRoles.{ADMIN, SUPERUSER, USER}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.Enrolment

object StrideAuthorisationPredicateForGatekeeperRole {

  def apply(strideAuthRoles: StrideAuthRoles)(strideRoleRequired: GatekeeperStrideRole): Predicate = {
    import strideAuthRoles._

    val adminEnrolment          = Enrolment(adminRole)
    lazy val superUserEnrolment = Enrolment(superUserRole)
    lazy val userEnrolment      = Enrolment(userRole)

    strideRoleRequired match {
      case ADMIN     => adminEnrolment
      case SUPERUSER => adminEnrolment or superUserEnrolment
      case USER      => adminEnrolment or superUserEnrolment or userEnrolment
    }
  }
}
