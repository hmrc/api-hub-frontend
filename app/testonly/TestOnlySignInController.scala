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

package testonly

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import play.api.data.Form
import play.api.data.Forms.{email, list, mapping, optional, text}
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testonly.TestOnlySignInController._
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TestOnlySignIn

import java.net.URL
import scala.util.{Failure, Success, Try}

@Singleton
class TestOnlySignInController @Inject()(
  override val controllerComponents: MessagesControllerComponents,
  testOnlySignInView: TestOnlySignIn,
  config: FrontendAppConfig
) extends FrontendBaseController {

  def showSignIn: Action[AnyContent] = Action {
    implicit request =>
      Ok(testOnlySignInView(signInFormWithRedirectUrl(config.loginContinueUrl)))
  }

  def submit(): Action[AnyContent] = Action {
    implicit request =>
      signInForm.bindFromRequest().fold(
        formWithErrors => BadRequest(testOnlySignInView(formWithErrors)),
        data => {
          val retrievals = Json.stringify(
            Json.toJson(
              Retrievals(data.principal, data.email)
            )
          )
          Redirect(data.redirectUrl).withSession(
            SessionKeys.authToken -> s"$AUTHORISED_TOKEN$retrievals"
          )
        }
      )
  }

  def auth: Action[AnyContent] = Action { implicit request =>
    request.headers.get(HeaderNames.authorisation) match {
      case Some(authorisation) if authorisation.startsWith(AUTHORISED_TOKEN) =>
        val json = authorisation.drop(AUTHORISED_TOKEN.length)
        Json.parse(json).validate[Retrievals] match {
          case JsSuccess(_, _) => Ok(json)
          case _ => Unauthorized
        }
      case _ => Unauthorized
    }
  }

}

case class Token(value: String) extends AnyVal

object TestOnlySignInController {

  val AUTHORISED_TOKEN: String = "AUTHORISED"

  val signInForm: Form[TestOnlySignInData] = Form(
    mapping(
      "principal" -> text
        .verifying("fake-sign-in.username.error.required", _.trim.nonEmpty),
      "email" -> optional(email),
      "token" -> optional(
        text.transform(Token.apply, unlift(Token.unapply))
      ),
      "redirectUrl" -> text
        .verifying("fake-sign-in.redirectUrl.error.required", value => Option(value).nonEmpty && value.trim.nonEmpty)
        .verifying("fake-sign-in.redirectUrl.error.absolute", value => isAbsoluteUrl(value.trim)),
      "permissions" -> list(
        mapping(
          "resourceTypes"     -> text,
          "resourceLocations" -> text,
          "actions"           -> text.transform[List[String]](_.split(",").toList, _.mkString(","))
        )(Permission.apply)(Permission.unapply))
        .transform[List[Permission]](_.filter(p => p.resourceType.nonEmpty && p.resourceLocation.nonEmpty), identity)
    )(TestOnlySignInData.apply)(TestOnlySignInData.unapply)
  )

  def signInFormWithRedirectUrl(redirectUrl: String): Form[TestOnlySignInData] =
    signInForm.fill(
      TestOnlySignInData(
        principal = "",
        email = None,
        token = None,
        redirectUrl = redirectUrl,
        permissions = Nil
      )
    )

  private def isAbsoluteUrl(url: String): Boolean =
    Try(new URL(url)) match {
      case Failure(_) => false
      case Success(_) => RedirectUrl.isAbsoluteUrl(url)
    }

  final case class TestOnlySignInData(
    principal   : String,
    email       : Option[String],
    token       : Option[Token],
    redirectUrl : String,
    permissions : List[Permission]
  )

  def convert(data: TestOnlySignInData): TestOnlyAddTokenRequest =
    TestOnlyAddTokenRequest(
      data.token,
      data.principal,
      data.email,
      data.permissions.toSet
    )

}
