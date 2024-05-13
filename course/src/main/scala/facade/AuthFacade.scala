package facade

import dto.AuthUser
import model.{SecretToken, Session, SessionId}
import model.error.{ApiError, UserAlreadyExistException}
import service.UserService
import zio.{Task, ZIO}

import scala.concurrent.duration.DurationInt

trait AuthFacade {
  def authenticate(authUser: AuthUser, secretToken: SecretToken): Task[Either[ApiError, Session]]
  def register(authUser: AuthUser): Task[Either[ApiError, Unit]]
}

class AuthFacadeImpl(sessionFacade: SessionFacade, userService: UserService) extends AuthFacade {
  override def authenticate(authUser: AuthUser, secretToken: SecretToken): Task[Either[ApiError, Session]] =
    for {
      userIdOpt <- userService.authenticate(authUser.login, authUser.password)
      res <- userIdOpt match {
        case Left(e) => ZIO.succeed(Left(e))
        case Right(userId) =>
          sessionFacade.issueSession(userId = userId, ttl = 10.minute, secretToken =  secretToken).map(Right(_))
      }
    } yield res

  override def register(authUser: AuthUser): Task[Either[ApiError, Unit]] = {
    val res: Task[Either[ApiError, Unit]] =
      userService.create(authUser)
        .asRight
        .catchSome {
          case _: UserAlreadyExistException => ZIO.succeed(Left(ApiError.UserAlreadyExist))
        }
    res
  }
}
