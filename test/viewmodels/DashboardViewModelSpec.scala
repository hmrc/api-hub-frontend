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

package viewmodels

import config.FrontendAppConfig
import controllers.actions.FakeUser
import generators.{ApiDetailGenerators, ApplicationGenerator, TeamGenerator}
import models.api.ApiDetail
import models.application.Application
import models.team.Team
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.{Configuration, Environment}
import play.api.i18n.{DefaultMessagesApi, Lang, Messages}

class DashboardViewModelSpec extends AnyFreeSpec with Matchers {

  import DashboardViewModelSpec.*

  "DashboardViewModel" - {
    "must build correctly when there are no applications" in {
      val actual = new DashboardViewModel(frontendAppConfig, Seq.empty, buildTeams(1), buildApis(1), FakeUser)

      actual.myApplicationsTitle mustBe "dashboard.applications.title.none"
      actual.showMyApplicationsEmptyMessage mustBe true
      actual.createApplicationMessage mustBe "dashboard.applications.createFirst"
      actual.createApplicationButtonIsSecondary mustBe false
      actual.myApplications mustBe empty
      actual.myApplicationsCount mustBe 0
      actual.showMyApplicationsLink mustBe false
    }

    "must build correctly when there is one application" in {
      val applications = buildApplications(1)
      val actual = new DashboardViewModel(frontendAppConfig, applications, Seq.empty, Seq.empty, FakeUser)

      actual.myApplicationsTitle mustBe "dashboard.applications.title.one"
      actual.showMyApplicationsEmptyMessage mustBe false
      actual.createApplicationMessage mustBe "dashboard.applications.createAnother"
      actual.createApplicationButtonIsSecondary mustBe true
      actual.myApplications mustBe applications
      actual.myApplicationsCount mustBe applications.size
      actual.showMyApplicationsLink mustBe false
    }

    "must build correctly when there are five applications" in {
      val applications = buildApplications(5)
      val actual = new DashboardViewModel(frontendAppConfig, applications, Seq.empty, Seq.empty, FakeUser)

      actual.myApplicationsTitle mustBe "dashboard.applications.title.many"
      actual.showMyApplicationsEmptyMessage mustBe false
      actual.createApplicationMessage mustBe "dashboard.applications.createAnother"
      actual.createApplicationButtonIsSecondary mustBe true
      actual.myApplications mustBe applications.sortBy(_.created).reverse
      actual.myApplicationsCount mustBe applications.size
      actual.showMyApplicationsLink mustBe false
    }

    "must build correctly when there are more than five applications" in {
      val applications = buildApplications(6)
      val actual = new DashboardViewModel(frontendAppConfig, applications, Seq.empty, Seq.empty, FakeUser)

      actual.myApplicationsTitle mustBe "dashboard.applications.title.many"
      actual.showMyApplicationsEmptyMessage mustBe false
      actual.createApplicationMessage mustBe "dashboard.applications.createAnother"
      actual.createApplicationButtonIsSecondary mustBe true
      actual.myApplications mustBe applications.sortBy(_.created).reverse.take(5)
      actual.myApplicationsCount mustBe applications.size
      actual.showMyApplicationsLink mustBe true
    }

    "must build correctly when there are no teams" in {
      val actual = new DashboardViewModel(frontendAppConfig, buildApplications(1), Seq.empty, buildApis(1), FakeUser)

      actual.myTeamsTitle mustBe "dashboard.teams.title.none"
      actual.showMyTeamsEmptyMessage mustBe true
      actual.createTeamMessage mustBe "dashboard.teams.createFirst"
      actual.createTeamButtonIsSecondary mustBe false
      actual.myTeams mustBe empty
      actual.myTeamsCount mustBe 0
      actual.showMyTeamsLink mustBe false
    }

    "must build correctly when there is one team" in {
      val teams = buildTeams(1)
      val actual = new DashboardViewModel(frontendAppConfig, Seq.empty, teams, buildApis(1), FakeUser)

      actual.myTeamsTitle mustBe "dashboard.teams.title.one"
      actual.showMyTeamsEmptyMessage mustBe false
      actual.createTeamMessage mustBe "dashboard.teams.createAnother"
      actual.createTeamButtonIsSecondary mustBe true
      actual.myTeams mustBe teams
      actual.myTeamsCount mustBe teams.size
      actual.showMyTeamsLink mustBe false
    }

    "must build correctly when there are five teams" in {
      val teams = buildTeams(5)
      val actual = new DashboardViewModel(frontendAppConfig, Seq.empty, teams, buildApis(1), FakeUser)

      actual.myTeamsTitle mustBe "dashboard.teams.title.many"
      actual.showMyTeamsEmptyMessage mustBe false
      actual.createTeamMessage mustBe "dashboard.teams.createAnother"
      actual.createTeamButtonIsSecondary mustBe true
      actual.myTeams mustBe teams.sortBy(_.created).reverse
      actual.myTeamsCount mustBe teams.size
      actual.showMyTeamsLink mustBe false
    }

    "must build correctly when there are more than five teams" in {
      val teams = buildTeams(6)
      val actual = new DashboardViewModel(frontendAppConfig, Seq.empty, teams, buildApis(1), FakeUser)

      actual.myTeamsTitle mustBe "dashboard.teams.title.many"
      actual.showMyTeamsEmptyMessage mustBe false
      actual.createTeamMessage mustBe "dashboard.teams.createAnother"
      actual.createTeamButtonIsSecondary mustBe true
      actual.myTeams mustBe teams.sortBy(_.created).reverse.take(5)
      actual.myTeamsCount mustBe teams.size
      actual.showMyTeamsLink mustBe true
    }

    "must build correctly when there are no APIs" in {
      val actual = new DashboardViewModel(frontendAppConfig, buildApplications(1), buildTeams(1), Seq.empty, FakeUser)

      actual.myApisTitle mustBe "dashboard.apis.title.none"
      actual.showMyApisEmptyMessage mustBe true
      actual.createApiMessage mustBe "dashboard.apis.createFirst"
      actual.createApisButtonIsSecondary mustBe false
      actual.myApis mustBe empty
      actual.myApisCount mustBe 0
      actual.showMyApisLink mustBe false
    }

    "must build correctly when there is one API" in {
      val apis = buildApis(1)
      val actual = new DashboardViewModel(frontendAppConfig, Seq.empty, Seq.empty, apis, FakeUser)

      actual.myApisTitle mustBe "dashboard.apis.title.one"
      actual.showMyApisEmptyMessage mustBe false
      actual.createApiMessage mustBe "dashboard.apis.createAnother"
      actual.createApisButtonIsSecondary mustBe true
      actual.myApis mustBe apis
      actual.myApisCount mustBe apis.size
      actual.showMyApisLink mustBe false
    }

    "must build correctly when there is five APIs" in {
      val apis = buildApis(5)
      val actual = new DashboardViewModel(frontendAppConfig, Seq.empty, Seq.empty, apis, FakeUser)

      actual.myApisTitle mustBe "dashboard.apis.title.many"
      actual.showMyApisEmptyMessage mustBe false
      actual.createApiMessage mustBe "dashboard.apis.createAnother"
      actual.createApisButtonIsSecondary mustBe true
      actual.myApis mustBe apis
      actual.myApisCount mustBe apis.size
      actual.showMyApisLink mustBe false
    }

    "must build correctly when there are more than five APIs" in {
      val apis = buildApis(6)
      val actual = new DashboardViewModel(frontendAppConfig, Seq.empty, Seq.empty, apis, FakeUser)

      actual.myApisTitle mustBe "dashboard.apis.title.many"
      actual.showMyApisEmptyMessage mustBe false
      actual.createApiMessage mustBe "dashboard.apis.createAnother"
      actual.createApisButtonIsSecondary mustBe true
      actual.myApis mustBe apis.take(5)
      actual.myApisCount mustBe apis.size
      actual.showMyApisLink mustBe true
    }
  }

}

object DashboardViewModelSpec extends ApiDetailGenerators with TeamGenerator with ApplicationGenerator {

  private implicit val messages: Messages = new DefaultMessagesApi().preferred(Seq(Lang("en")))
  private val frontendAppConfig: FrontendAppConfig = new FrontendAppConfig(Configuration.load(Environment.simple()))

  def buildApplications(count: Int): Seq[Application] = {
    (1 to count).map(
      _ =>
        sampleApplication()
    )
  }

  def buildTeams(count: Int): Seq[Team] = {
    (1 to count).map(
      _ =>
        sampleTeam()
    )
  }

  def buildApis(count: Int): Seq[ApiDetail] = {
    (1 to count).map(
      _ =>
        sampleApiDetail()
    )
  }

}
