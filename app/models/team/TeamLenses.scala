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

package models.team

import models.Lens
import models.application.TeamMember
import models.user.UserModel

object TeamLenses {

  val teamTeamMembers: Lens[Team, Seq[TeamMember]] =
    Lens[Team, Seq[TeamMember]](
      get = _.teamMembers,
      set = (team, teamMembers) => team.copy(teamMembers = teamMembers)
    )

  implicit class TeamLensOps(team: Team) {

    def setTeamMembers(teamMembers: Seq[TeamMember]): Team = {
      teamTeamMembers.set(team, teamMembers)
    }

    def addTeamMember(teamMember: TeamMember): Team = {
      teamTeamMembers.set(
        team,
        team.teamMembers :+ teamMember
      )
    }

    def addTeamMember(email: String): Team = {
      addTeamMember(TeamMember(email))
    }

    def hasTeamMember(email: String): Boolean = {
      team.teamMembers.exists(_.email.equalsIgnoreCase(email))
    }

    def hasTeamMember(teamMember: TeamMember): Boolean = {
      hasTeamMember(teamMember.email)
    }

    def withSortedTeam(): Team = {
      setTeamMembers(team.teamMembers.sortWith(_.email.toUpperCase() < _.email.toUpperCase()))
    }

    def isAccessible(user: UserModel): Boolean = {
      user.permissions.canSupport ||
        hasTeamMember(user.email)
    }

  }

}
