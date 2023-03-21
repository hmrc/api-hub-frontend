package controllers.actions
import models.requests.{ApplicationRequest, IdentifierRequest}
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

class FakeApplicationAuthActionProvider extends ApplicationAuthActionProvider {

  override def apply(applicationId: String)(implicit ec: ExecutionContext): ApplicationAuthAction = {
    new ApplicationAuthAction {
      override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, ApplicationRequest[A]]] = {
        Future.successful(Right(
          ApplicationRequest(
            request,
            FakeApplication
          )
        ))
      }

      override protected def executionContext: ExecutionContext = ec
    }
  }

}
