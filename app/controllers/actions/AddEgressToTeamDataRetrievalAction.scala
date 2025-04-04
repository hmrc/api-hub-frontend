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

import models.requests.{IdentifierRequest, OptionalDataRequest}
import play.api.mvc.ActionTransformer
import repositories.AddEgressToTeamSessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait AddEgressToTeamDataRetrievalAction extends ActionTransformer[IdentifierRequest, OptionalDataRequest]

class AddEgressToTeamDataRetrievalActionImpl @Inject()(
  sessionRepository: AddEgressToTeamSessionRepository
)(implicit val executionContext: ExecutionContext) extends AddEgressToTeamDataRetrievalAction {

  override protected def transform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = {
    sessionRepository.get(request.user.userId).map {
      OptionalDataRequest(request.request, request.user, _)
    }
  }

}
