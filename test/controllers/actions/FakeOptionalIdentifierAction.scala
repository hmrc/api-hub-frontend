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
import models.requests.OptionalIdentifierRequest
import play.api.mvc.{AnyContent, BodyParser, PlayBodyParsers, Request, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FakeOptionalIdentifierAction @Inject()(
  optionalUserProvider: OptionalUserProvider,
  bodyParsers: PlayBodyParsers
)(implicit override val executionContext: ExecutionContext) extends OptionalIdentifierAction{

  override val parser: BodyParser[AnyContent] = bodyParsers.default

  override def invokeBlock[A](request: Request[A], block: OptionalIdentifierRequest[A] => Future[Result]): Future[Result] = {
    block(OptionalIdentifierRequest(request, optionalUserProvider.get()))
  }

}
