package facade

import dto.AuthUser
import model.User
import model.auth.Session
import model.error.AuthError
import service.{SessionService, UserService}
import zio.IO

import java.time.Instant
import scala.concurrent.duration.DurationInt

trait AuthFacade {
  def authenticateAndIssueSession(authUser : AuthUser): IO[AuthError, Session]
  def register(authUser: AuthUser): IO[AuthError, Unit]
}




class AuthFacadeImpl(userService : UserService, sessionService: SessionService) extends AuthFacade {

  override def authenticateAndIssueSession(authUser: AuthUser): IO[AuthError, Session] = {
    for {
      userId <- userService.authenticate(authUser.login , authUser.password)
      session <- sessionService.issueSession(userId, createdAt = Instant.now() , ttl = 10.minute).orDie
    } yield session
  }

  override def register(authUser: AuthUser): IO[AuthError, Unit] =
    userService.register(authUser)
}
