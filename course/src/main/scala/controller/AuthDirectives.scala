package controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, Directive0}
import facade.SessionFacade
import model.error.SessionError
import model.{Login, SecretToken, SessionId}
import utils.ZIOFutures._

class AuthDirectives(sessionFacade: SessionFacade) {

  def checkSessionDirective(
      sessionId: SessionId,
      secretToken: SecretToken,
      login: Login,
      invalidateCookie: Boolean): Directive0 = {
    onSuccess(
      sessionFacade.checkSession(sessionId = sessionId, maybeSessionOwner = login, secretToken).unsafeToFuture
    ).flatMap {
      case Left(authError) => authError match {
          // add cookie invalidation
          case t: SessionError =>
            (
              if (invalidateCookie)
                deleteSessionCookie
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
