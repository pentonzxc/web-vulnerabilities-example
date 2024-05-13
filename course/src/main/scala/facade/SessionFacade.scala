package facade

import model.auth.Session
import model.error.{AuthError, InvalidUserException}
import model.{Login, SecretToken, SessionId, UserId}
import service.{SessionService, UserService}
import zio.{Task, ZIO}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

trait SessionFacade {
  def checkSession(
      sessionId: SessionId,
      secretToken: SecretToken,
      maybeSessionOwner: Login,
      time: Instant = Instant.now()): Task[Either[AuthError, Session]]
  def issueSession(secretToken : SecretToken, userId: UserId, ttl: FiniteDuration) : Task[Session]
  def invalidateSession(sessionId: SessionId): Task[Unit]
}

class SessionFacadeImpl(sessionService: SessionService, userService: UserService) extends SessionFacade {

  override def checkSession(
      sessionId: SessionId,
      secretToken: SecretToken,
      maybeSessionOwner: Login,
      time: Instant = Instant.now()): Task[Either[AuthError, Session]] = {
    for {
      ownerOpt <- userService.findUserByLogin(maybeSessionOwner)
      sessionOpt <- sessionService.findSession(sessionId)

      result = for {
        session <- sessionOpt.toRight(AuthError.InvalidSession)
        _ <- checkExpired(session, time)
        owner = ownerOpt.getOrElse(throw new InvalidUserException)
        _ <- checkStolen(session, owner.id)
        _ <- checkSecret(session, secretToken)
      } yield session

      _ <- ZIO.whenCase(result) {
        case Left(AuthError.StolenSession) => invalidateSession(sessionId)
      }
    } yield result
  }

  override def invalidateSession(sessionId: SessionId): Task[Unit] =
    sessionService.invalidateSession(sessionId)

  private def checkExpired(session: Session, now: Instant) =
    Either.cond(session.exp.isAfter(now), (), AuthError.ExpiredSession)

  private def checkStolen(session: Session, maybeIss: UserId) =
    Either.cond(session.iss == maybeIss, (), AuthError.StolenSession)

  private def checkSecret(session : Session, secret : SecretToken) =
    Either.cond(session.secretToken == secret, (), AuthError.InvalidCsrf)

  override def issueSession(secretToken : SecretToken, userId: UserId, ttl: FiniteDuration): Task[Session] =
    sessionService.issueSession(userId, secretToken, createdAt = Instant.now(), ttl = ttl)
}
