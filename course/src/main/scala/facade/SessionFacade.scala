package facade

import model.error.{ApiError, InvalidUserException}
import model.{Login, SecretToken, Session, SessionId, UserId}
import service.{SessionService, UserService}
import zio.{Task, ZIO}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

trait SessionFacade {
  def checkSession(
      sessionId: SessionId,
      secretToken: SecretToken,
      maybeSessionOwner: Login,
      time: Instant = Instant.now()): Task[Either[ApiError, Session]]
  def issueSession(secretToken : SecretToken, userId: UserId, ttl: FiniteDuration) : Task[Session]
  def invalidateSession(sessionId: SessionId): Task[Unit]
}

class SessionFacadeImpl(sessionService: SessionService, userService: UserService) extends SessionFacade {

  override def checkSession(
      sessionId: SessionId,
      secretToken: SecretToken,
      maybeSessionOwner: Login,
      time: Instant = Instant.now()): Task[Either[ApiError, Session]] = {
    for {
      ownerOpt <- userService.findUserByLogin(maybeSessionOwner)
      sessionOpt <- sessionService.findSession(sessionId)

      result = for {
        session <- sessionOpt.toRight(ApiError.InvalidSession)
        owner <- ownerOpt.toRight(ApiError.InvalidUser)
        _ <- checkExpired(session, time)
        _ <- checkStolen(session, owner.id)
        _ <- checkSecret(session, secretToken)
      } yield session

      _ <- ZIO.whenCase(result) {
        case Left(ApiError.StolenSession) => invalidateSession(sessionId)
      }
    } yield result
  }

  override def issueSession(secretToken: SecretToken, userId: UserId, ttl: FiniteDuration): Task[Session] =
    sessionService.issueSession(userId, secretToken, createdAt = Instant.now(), ttl = ttl)

  override def invalidateSession(sessionId: SessionId): Task[Unit] =
    sessionService.invalidateSession(sessionId)

  private def checkExpired(session: Session, now: Instant) =
    Either.cond(session.exp.isAfter(now), (), ApiError.ExpiredSession)

  private def checkStolen(session: Session, maybeIss: UserId) =
    Either.cond(session.iss == maybeIss, (), ApiError.StolenSession)

  private def checkSecret(session : Session, secret : SecretToken) =
    Either.cond(session.secretToken == secret, (), ApiError.InvalidCsrf)
}
