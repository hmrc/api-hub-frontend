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

package viewmodels.admin

import config.HipEnvironment
import controllers.actions.FakeUser
import fakes.FakeHipEnvironments
import forms.admin.ForcePublishPublisherReferenceFormProvider
import models.api.ApiDeploymentStatus
import models.api.ApiDeploymentStatus.{Deployed, NotDeployed, Unknown}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.data.Form
import play.api.i18n.Messages
import play.api.test.Helpers

class ForcePublishViewModelSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  import ForcePublishViewModelSpec.*

  "hasVersionComparison" - {
    "must return true when there is a publisher reference else false" in {
      val tests = Table(
        ("publisherReference", "expected"),
        (Some(publisherReference), true),
        (None, false)
      )

      forAll(tests) {(publisherReference: Option[String], expected: Boolean) =>
        val viewModel = ForcePublishViewModel(
          form = form,
          user = FakeUser,
          publisherReference = publisherReference
        )

        viewModel.hasVersionComparison mustBe expected
      }
    }
  }

  "deployedVersionText" - {
    "must return the correct value for the API deployment status" in {
      val tests = Table(
        ("status", "expected"),
        (Some(Deployed(environment.id, deployedVersion)), deployedVersion),
        (Some(NotDeployed(environment.id)), "forcePublish.versionComparison.version.notDeployed"),
        (Some(Unknown(environment.id)), "forcePublish.versionComparison.version.unknown"),
        (None, "forcePublish.versionComparison.version.unknown")
      )

      forAll(tests) {(status: Option[ApiDeploymentStatus], expected: String) =>
        val viewModel = ForcePublishViewModel(
          form = form,
          user = FakeUser,
          deploymentStatus = status
        )

        viewModel.deployedVersionText mustBe expected
      }
    }
  }

  "catalogueVersionText" - {
    "must return the correct value for the API catalogue version" in {
      val tests = Table(
        ("version", "expected"),
        (Some(catalogueVersion), catalogueVersion),
        (None, "forcePublish.versionComparison.version.notPublished")
      )

      forAll(tests) {(version: Option[String], expected: String) =>
        val viewModel = ForcePublishViewModel(
          form = form,
          user = FakeUser,
          catalogueVersion = version
        )

        viewModel.catalogueVersionText mustBe expected
      }
    }
  }

  "canForcePublish" - {
    "must return true if there is a deployment to publish else false" in {
      val tests = Table(
        ("status", "expected"),
        (Some(Deployed(environment.id, deployedVersion)), true),
        (Some(NotDeployed(environment.id)), false),
        (Some(Unknown(environment.id)), false),
        (None, false)
      )

      forAll(tests) {(status: Option[ApiDeploymentStatus], expected: Boolean) =>
        val viewModel = ForcePublishViewModel(
          form = form,
          user = FakeUser,
          deploymentStatus = status
        )

        viewModel.canForcePublish mustBe expected
      }
    }
  }

}

private object ForcePublishViewModelSpec {

  implicit val messages: Messages = Helpers.stubMessages()
  val form: Form[?] = new ForcePublishPublisherReferenceFormProvider()()
  val publisherReference = "test-publisher-reference"
  val deployedVersion = "test-deployed-version"
  val catalogueVersion = "test-catalogue-version"
  val environment: HipEnvironment = FakeHipEnvironments.test

}