package facade

import dto.AuthUser
import model.auth.Session
import model.error.{AuthError, UserAlreadyExist}
import service.{SessionService, UserService}
import zio.{Task, ZIO}

import java.time.Instant
import scala.concurrent.duration.DurationInt

trait AuthFacade {
  def authenticate(authUser: AuthUser): Task[Either[AuthError, Session]]
  def register(authUser: AuthUser): Task[Either[AuthError, Unit]]
}

class AuthFacadeImpl(userService: UserService, sessionService: SessionService) extends AuthFacade {

  override def authenticate(authUser: AuthUser): Task[Either[AuthError, Session]] =
    for {
      userIdOpt <- userService.authenticate(authUser.login, authUser.password)
      res <- userIdOpt match {
        case Left(e) => ZIO.succeed(Left(e))
        case Right(userId) =>
          sessionService.issueSession(userId, createdAt = Instant.now(), ttl = 10.minute).map(Right(_))
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
}
