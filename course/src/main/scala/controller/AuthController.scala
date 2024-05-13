package controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.AuthUser
import facade.{AuthFacade, SessionFacade}
import model.error.ApiError
import model.{SecretToken, SessionId}
import utils.ZIOFutures._
import zio.ZIO

class AuthController(authFacade: AuthFacade, sessionFacade: SessionFacade)
  extends Controller with StrictLogging {

  private val register: Route = post {
    (path("register") & entity(as[AuthUser])) { authUser =>
      onSuccess(authFacade.register(authUser).unsafeToFuture) {
        case Right(_) => complete(StatusCodes.OK)

        case Left(err: ApiError.UserAlreadyExist.type) => complete(StatusCodes.Conflict, err.message)
        case _ => complete(StatusCodes.InternalServerError)
      }
    }
  }

  private val login: Route = post {
    (path("login") & entity(as[AuthUser])) { authUser =>
      (sessionCookieOpt & optionalHeaderValueByType[`X-CSRF-TOKEN`]()) { case (sessionOpt, csrfOpt) =>
        loginRoute(authUser, sessionOpt, secretToken = csrfOpt.map(_.token))
      }
    }
  }

  override val route: Route =
    concat(
      login,
      register
    )

  private def loginRoute(
      authUser: AuthUser,
      sessionId: Option[SessionId],
      secretToken: Option[SecretToken]
  ): Route = {
    val authenticator = sessionId.zip(secretToken) match {
      case Some((sessionId, secretToken)) =>
        sessionFacade.checkSession(sessionId, secretToken, authUser.login).flatMap {
          case Left(_) =>
            logger.debug("Can't use session for logging")
            authFacade.authenticate(authUser, generateCsrfToken())

          case Right(session) =>
            ZIO.attempt(Right(session))
        }
      case None =>
        logger.debug("Can't use session for logging")
        authFacade.authenticate(authUser, generateCsrfToken())
    }

    onSuccess(authenticator.unsafeToFuture) {
      case Right(session) =>
        (respondWithHeader(`X-CSRF-TOKEN`(session.secretToken)) & setCookie(sessionCookie(
          session.id,
          exp = session.exp))) {
          complete(StatusCodes.OK)
        }
      case Left(err) =>
        removeSessionCookie {
          complete(StatusCodes.Forbidden, err.message)
        }
    }
  }
}
