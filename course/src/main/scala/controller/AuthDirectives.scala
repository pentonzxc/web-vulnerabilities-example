package controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, Directive0}
import com.typesafe.scalalogging.StrictLogging
import facade.SessionFacade
import model.error.ApiError._
import model.{Login, SecretToken, SessionId}
import utils.ZIOFutures._

class AuthDirectives(sessionFacade: SessionFacade) extends StrictLogging {
  def checkSessionDirective(
      sessionId: SessionId,
      secretToken: SecretToken,
      login: Login,
      invalidateSessionCookie: Boolean): Directive0 = {
    onSuccess(
      sessionFacade.checkSession(
        sessionId = sessionId,
        secretToken = secretToken,
        maybeSessionOwner = login).unsafeToFuture
    ).flatMap {
      case Left(err) => err match {
          case InvalidUser | InvalidCsrf =>
            complete(StatusCodes.BadRequest, err.message)
          case InvalidSession | ExpiredSession | StolenSession =>
            (
              if (invalidateSessionCookie)
                removeSessionCookie
              else
                Directive.Empty
            ).tflatMap { _ =>
              complete(StatusCodes.Forbidden, err.message)
            }
          case _ =>
            logger.debug(s"Unexpected error: $err")
            complete(StatusCodes.InternalServerError)
        }
      case Right(_) => pass
    }
  }
}
