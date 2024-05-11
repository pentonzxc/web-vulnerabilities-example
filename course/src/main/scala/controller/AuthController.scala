package controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.AuthUser
import facade.{AuthFacade, SessionFacade}
import model.SessionId
import model.error.AuthError
import utils.ZIOFutures._
import zio.ZIO

class AuthController(authFacade: AuthFacade, sessionFacade: SessionFacade) extends Controller {

  val sessionCookieOpt = optionalCookie("session").map(_.map(c => SessionId(c.value)))

  private val register: Route = post {
    (path("register") & entity(as[AuthUser])) { authUser =>
      onSuccess(authFacade.register(authUser).unsafeToFuture) {
        case Right(_) => complete(StatusCodes.OK)

        case Left(AuthError.UserAlreadyExist) => complete(StatusCodes.Conflict)
        case _ => complete(StatusCodes.InternalServerError)
      }
    }
  }

  private val login: Route = post {
    (path("login") & entity(as[AuthUser])) { authUser =>
      sessionCookieOpt { sessionOpt =>
        val authenticateAndCreateSession = ZIO.succeed(sessionOpt).flatMap {
          case Some(session) =>
            // reuse session
            sessionFacade.checkSessionWithOwner(session, authUser.login).foldZIO(
              failure = _ => authFacade.authenticate(authUser),
              success = ZIO.attempt(_)
            )

          case None => authFacade.authenticate(authUser)
        }.map(_.map(_.id))

        onSuccess(authenticateAndCreateSession.unsafeToFuture) {
          case Right(session) =>
            setCookie(HttpCookie("session", session.value)) {
              complete(StatusCodes.OK)
            }

          case Left(AuthError.InvalidPassword) => complete(StatusCodes.Unauthorized)
          case Left(AuthError.InvalidUser) => complete(StatusCodes.BadRequest)
          case _ => complete(StatusCodes.InternalServerError)
        }
      }
    }
  }

  override val route: Route =
    concat(
      login,
      register
    )
}
