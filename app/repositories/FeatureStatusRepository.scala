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

import com.google.inject.{Inject, Singleton}
import com.mongodb.client.model.UpdateOptions
import models.hubstatus.{Feature, FeatureStatus}
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, Updates}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureStatusRepository @Inject()(
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext) extends PlayMongoRepository[FeatureStatus] (
  collectionName = "feature-status",
  mongoComponent = mongoComponent,
  domainFormat = FeatureStatus.formatFeatureStatus,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("feature"),
      IndexOptions()
        .name("featureIdx")
        .unique(true)
    )
  )
) {

  override lazy val requiresTtlIndex = false // We do not want to expire data

  def upsert(featureStatus: FeatureStatus): Future[Unit] = {
    Mdc.preservingMdc {
      collection.updateOne(
        filter = Filters.equal("feature", featureStatus.feature.toString),
        update = Updates.combine(
          Updates.set("feature", featureStatus.feature.toString),
          Updates.set("shuttered", featureStatus.shuttered),
          featureStatus.shutterMessage match {
            case Some(shutterMessage) => Updates.set("shutterMessage", shutterMessage)
            case None => Updates.unset("shutterMessage")
          }
        ),
        options = UpdateOptions().upsert(true)
      )
        .toFuture()
        .map(_ => ())
    }
  }

  def get(feature: Feature): Future[Option[FeatureStatus]] = {
    Mdc.preservingMdc {
      collection
        .find(Filters.equal("feature", feature.toString))
        .toFuture()
        .map(_.headOption)
    }
  }

}
