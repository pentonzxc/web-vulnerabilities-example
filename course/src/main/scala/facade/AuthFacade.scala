package facade

import dto.AuthUser
import model.{SecretToken, SessionId}
import model.auth.Session
import model.error.{AuthError, UserAlreadyExist}
import service.UserService
import zio.{Task, ZIO}

import scala.concurrent.duration.DurationInt

trait AuthFacade {
  def authenticate(authUser: AuthUser, secretToken: SecretToken): Task[Either[AuthError, Session]]

  def register(authUser: AuthUser): Task[Either[AuthError, Unit]]

  def useSessionOrFallbackToAuthentication(
      sessionOpt: Option[SessionId],
      authUser: AuthUser,
      secretToken : SecretToken
  ): Task[Either[AuthError, Session]]
}

class AuthFacadeImpl(sessionFacade: SessionFacade, userService: UserService) extends AuthFacade {

  override def authenticate(authUser: AuthUser, secretToken: SecretToken): Task[Either[AuthError, Session]] =
    for {
      userIdOpt <- userService.authenticate(authUser.login, authUser.password)
      res <- userIdOpt match {
        case Left(e) => ZIO.succeed(Left(e))
        case Right(userId) =>
          sessionFacade.issueSession(userId = userId, ttl = 10.minute, secretToken =  secretToken).map(Right(_))
      }
    } yield res

  override def register(authUser: AuthUser): Task[Either[AuthError, Unit]] = {
    val res: Task[Either[AuthError, Unit]] =
      userService.create(authUser)
        .asRight
        .catchSome {
          case _: UserAlreadyExist => ZIO.succeed(Left(AuthError.UserAlreadyExist))
        }
    res
  }

  override def useSessionOrFallbackToAuthentication(
      sessionOpt: Option[SessionId],
      authUser: AuthUser,
      secretToken: SecretToken): Task[Either[AuthError, Session]] = {
    def withFallback(getSession: Task[Either[AuthError, Session]]): Task[Either[AuthError, Session]] =
      getSession.foldZIO(
        failure = _ => authenticate(authUser, secretToken),
        success = {
          case Left(err) =>
            zio.Console.printLine(s"Can't use source session, $err").orDie *>
              authenticate(authUser, secretToken)

          case Right(session) =>
            ZIO.succeed(session).asRight
        }
      )

    val session = sessionOpt match {
      case Some(session) => withFallback {
          sessionFacade.checkSession(session, authUser.login, secretToken)
        }
      case None => authenticate(authUser, secretToken)
    }

    session
  }

}
