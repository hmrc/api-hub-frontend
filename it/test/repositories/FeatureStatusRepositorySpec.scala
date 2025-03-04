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

package repositories

import models.hubstatus.{Feature, FeatureStatus, FrontendShutter}
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.Future

class FeatureStatusRepositorySpec extends AsyncFreeSpec
  with Matchers
  with DefaultPlayMongoRepositorySupport[FeatureStatus]
  with OptionValues {

  override protected val repository: FeatureStatusRepository = new FeatureStatusRepository(mongoComponent)

  "upsert" - {
    "must insert a record when none exists" in {
      val featureStatus = FeatureStatus(FrontendShutter, true, Some("test-shutter-message"))

      for {
        _ <- insert(featureStatus)
        _ <- repository.upsert(featureStatus)
        saved <- find(featureStatus.feature)
      } yield {
        saved.value mustBe featureStatus
      }
    }

    "must update a record when one exists" in {
      val featureStatus = FeatureStatus(FrontendShutter, true, Some("test-shutter-message"))
      val updated = featureStatus.copy(shuttered = false, shutterMessage = None)

      for {
        _ <- insert(featureStatus)
        _ <- repository.upsert(updated)
        saved <- find(featureStatus.feature)
      } yield {
        saved.value mustBe updated
      }
    }
  }

  "get" - {
    "must return the record when it exists" in {
      val featureStatus = FeatureStatus(FrontendShutter, true, Some("test-shutter-message"))

      for {
        _ <- insert(featureStatus)
        saved <- repository.get(featureStatus.feature)
      } yield {
        saved.value mustBe featureStatus
      }
    }

    "must return None when no record exists" in {
      for {
        saved <- repository.get(FrontendShutter)
      } yield {
        saved mustBe None
      }
    }
  }

  private def find(feature: Feature): Future[Option[FeatureStatus]] = {
      find(Filters.equal("feature", feature.toString))
        .map(_.headOption)
  }

}
