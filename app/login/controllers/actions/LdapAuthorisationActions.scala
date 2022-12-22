/*
 * Copyright 2022 HM Revenue & Customs
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

package login.controllers.actions

import login.domain.models.LoggedInRequest
import login.services._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait LdapAuthorisationActions {
  self: FrontendBaseController =>

  implicit def ec: ExecutionContext
  def ldapAuthorisationService: LdapAuthorisationService

  val anyAuthenticatedUserRefiner = new ActionRefiner[MessagesRequest, LoggedInRequest] {
    override def executionContext = ec

    override protected def refine[A](msgRequest: MessagesRequest[A]): Future[Either[Result, LoggedInRequest[A]]] = {
      def refineLdap = ldapAuthorisationService.refineLdap(msgRequest)
          .recover {
            case NonFatal(_) => Left(())
          }


      import cats.implicits._

      refineLdap.map( either => either.leftMap( any => Unauthorized("")))
    }
  }

  def anyAuthenticatedUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] = {
    Action.async { implicit request =>
      anyAuthenticatedUserRefiner.invokeBlock(request, block)
    }
  }
}
