package controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, Directive0}
import com.typesafe.scalalogging.StrictLogging
import facade.{AuthFacade, SessionFacade}
import model.error.SessionError
import model.{Login, SecretToken, SessionId}
import utils.ZIOFutures._

class AuthDirectives(sessionFacade: SessionFacade) extends StrictLogging {

  def checkSessionDirective(
      sessionId: SessionId,
      secretToken: SecretToken,
      login: Login,
      invalidateSessionCookie: Boolean): Directive0 = {
    onSuccess(
      sessionFacade.checkSession(sessionId = sessionId, secretToken = secretToken, maybeSessionOwner = login).unsafeToFuture
    ).flatMap {
      case Left(authError) => authError match {
          case t: SessionError =>
            (
              if (invalidateSessionCookie)
                removeSessionCookie
              else
                Directive.Empty
            ).tflatMap { _ =>
              complete(StatusCodes.Forbidden, t.message)
            }
          case _ =>
            complete(StatusCodes.InternalServerError)
        }
      case Right(_) => pass
    }
  }
}
