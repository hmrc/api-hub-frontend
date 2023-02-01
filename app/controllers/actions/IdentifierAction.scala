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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import play.api.{Configuration, Environment}
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.internalauth.client.Retrieval
//import uk.gov.hmrc.auth.core._
//import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject()(
//                                               override val authConnector: AuthConnector,
                                               auth: FrontendAuthComponents,
                                               configuration: FrontendAppConfig,
                                               val parser: BodyParsers.Default,
                                               override val config: Configuration,
                                               override val env: Environment
                                             )
                                             (implicit val executionContext: ExecutionContext)
  extends IdentifierAction
//    with AuthorisedFunctions
    with AuthRedirects {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    auth.authenticatedAction(routes.IndexController.onPageLoad, Retrieval.username) {
      implicit request => {
        val eventualResult = block(IdentifierRequest(request, request.retrieval.value))

//        eventualResult.value.getOrElse(throw new UnauthorizedException("Unable to retrieve user credentials")).get
        eventualResult;
      }.
    }


//    authorised().retrieve(Retrievals.credentials) {
//      _.map {
//        credentials => block(IdentifierRequest(request, s"${credentials.providerType}-${credentials.providerId}"))
//      }.getOrElse(throw new UnauthorizedException("Unable to retrieve user credentials"))
//    } recover {
//      case _: NoActiveSession =>
//        toStrideLogin(configuration.loginContinueUrl)
//      case _: AuthorisationException =>
//        Redirect(routes.UnauthorisedController.onPageLoad)
//    }
  }
}

class SessionIdentifierAction @Inject()(
                                         val parser: BodyParsers.Default
                                       )
                                       (implicit val executionContext: ExecutionContext) extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    hc.sessionId match {
      case Some(session) =>
        block(IdentifierRequest(request, session.value))
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
