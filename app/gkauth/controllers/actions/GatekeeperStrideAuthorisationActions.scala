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

package gkauth.controllers.actions

import gkauth.domain.models.GatekeeperRoles.USER
import gkauth.domain.models.{GatekeeperRoles, GatekeeperStrideRole, LoggedInRequest}
import gkauth.services.StrideAuthorisationService
import GatekeeperRoles.USER
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.ActionRefiner
import play.api.mvc.MessagesRequest
import gkauth.services._

import scala.util.control.NonFatal

trait ForbiddenHandler {
  def handle(msgResult: MessagesRequest[_]): Result
}

trait GatekeeperStrideAuthorisationActions {
  self: FrontendBaseController =>

  def strideAuthorisationService: StrideAuthorisationService

  implicit def ec: ExecutionContext

  def gatekeeperRoleActionRefiner(minimumRoleRequired: GatekeeperStrideRole): ActionRefiner[MessagesRequest, LoggedInRequest] =
    new ActionRefiner[MessagesRequest, LoggedInRequest] {
      def executionContext = ec

      def refine[A](msgRequest: MessagesRequest[A]): Future[Either[Result, LoggedInRequest[A]]] = {
        strideAuthorisationService.refineStride(minimumRoleRequired)(msgRequest)
      }
    }

  private def gatekeeperRoleAction(minimumRoleRequired: GatekeeperStrideRole)(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      gatekeeperRoleActionRefiner(minimumRoleRequired).invokeBlock(request, block)
    }

  def anyStrideUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRoles.USER)(block)

  def atLeastSuperUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRoles.SUPERUSER)(block)

  def adminOnlyAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRoles.ADMIN)(block)
}

trait GatekeeperAuthorisationActions {
  self: FrontendBaseController with GatekeeperStrideAuthorisationActions =>


  val anyAuthenticatedUserRefiner = new ActionRefiner[MessagesRequest, LoggedInRequest] {

    override def executionContext = ec

    override protected def refine[A](msgRequest: MessagesRequest[A]): Future[Either[Result, LoggedInRequest[A]]] = {
      type FERLIR = Future[Either[Result, LoggedInRequest[A]]]


      def refineStride: FERLIR =
        strideAuthorisationService.refineStride(USER)(msgRequest)
          .recover {
            case NonFatal(_) => Left(Unauthorized(""))
          }

      /*import cats.implicits._
      import cats.data.EitherT
      EitherT(refineStride).leftFlatMap { strideFailureResult =>
        EitherT(refineLdap).leftMap(_ => strideFailureResult)
      }
        .value*/
      refineStride
    }
  }

  def anyAuthenticatedUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] = {
    Action.async { implicit request =>
      (
        anyAuthenticatedUserRefiner
      )
        .invokeBlock(request, block)
    }
  }
}
