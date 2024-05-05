package routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.AuthUser
import facade.AuthFacade
import model.error.AuthError
import utils.ZIOFutures._

class AuthRoute(authFacade: AuthFacade) extends Router {

  private val register: Route =
    (path("register") & entity(as[AuthUser])) { authUser =>
      onSuccess(authFacade.register(authUser).either.unsafeToFuture) {
        case Right(_) => complete(StatusCodes.OK)

        case Left(AuthError.UserAlreadyExist) => complete(StatusCodes.Conflict)
        case _ => complete(StatusCodes.InternalServerError)
      }
    }

  private val login: Route =
    (path("login") & entity(as[AuthUser])) { authUser =>
      onSuccess(authFacade.authenticateAndIssueSession(authUser).either.unsafeToFuture) {
        case Right(session) =>
          setCookie(HttpCookie("session", session.id.value)) {
            complete(StatusCodes.OK)
          }

        case Left(AuthError.InvalidPassword) => complete(StatusCodes.Unauthorized)
        case Left(AuthError.NonExistUser) => complete(StatusCodes.BadRequest)
        case _ => complete(StatusCodes.InternalServerError)
      }
    }


  override val route: Route = path("auth") {
    concat(login, register)
  }
}
