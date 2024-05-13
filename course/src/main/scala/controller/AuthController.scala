package controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.AuthUser
import facade.{AuthFacade, SessionFacade}
import model.error.{AuthError, SessionError}
import model.{SecretToken, SessionId}
import utils.ZIOFutures._

import java.security.SecureRandom

class AuthController(authFacade: AuthFacade, sessionFacade: SessionFacade, authDirectives: AuthDirectives) extends Controller {

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
        onSuccess(authFacade.useSessionOrFallbackToAuthentication(
          sessionOpt,
          authUser,
          secretToken = Security.generateCsrfToken()).unsafeToFuture) {
          case Right(session) =>
            respondWithHeader(`X-CSRF-TOKEN`(session.secretToken)) {
              setCookie(HttpCookie("session", session.id.value)) {
                complete(StatusCodes.OK)
              }
            }
          case Left(err : SessionError) => complete(err.message)
          case Left(AuthError.InvalidPassword) => complete(StatusCodes.Forbidden)
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

  private object Security {

    private val threadLocalRandom: ThreadLocal[SecureRandom] = ThreadLocal.withInitial(() => new SecureRandom())
    def generateCsrfToken(): SecretToken = {
      val random = threadLocalRandom.get()
      val buffer = new StringBuilder
      val length = Alphanumeric.length

      (0 until 32).foreach { _ =>
        buffer += Alphanumeric.charAt(random.nextInt(length)).toString
      }

      SecretToken(buffer.toString())
    }

    private val Alphanumeric: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
  }
}
