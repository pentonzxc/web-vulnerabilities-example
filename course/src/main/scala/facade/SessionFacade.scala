package facade

import model.error.AuthError
import model.{Login, SessionId}
import service.{SessionService, UserService}
import zio.{Task, ZIO}

import java.time.Instant


// FIXME: make this class to reuse methods
trait SessionFacade {
  def checkSessionWithOwner(sessionId: SessionId, maybeSessionOwner: Login): Task[Either[AuthError, Unit]]

  def checkSession(sessionId: SessionId): Task[Either[AuthError, Unit]]

  def invalidateSession(sessionId: SessionId): Task[Unit]
}

class SessionFacadeImpl(sessionService: SessionService, userService: UserService) extends SessionFacade {

  override def checkSessionWithOwner(sessionId: SessionId, maybeSessionOwner: Login): Task[Either[AuthError, Unit]] =
    for {
      ownerOpt <- userService.findUserByLogin(maybeSessionOwner)
      sessionOpt <- sessionService.findSession(sessionId)

      checkResult = sessionOpt match {
        case Some(session) =>
          if (!ownerOpt.exists(_.id == session.iss))
            Left(AuthError.StolenSession)
          else if (session.exp.isBefore(Instant.now()))
            Left(AuthError.ExpiredSession)
          else
            Right(())
        case None =>
          Left(AuthError.InvalidSession)
      }

      _ <- ZIO.whenCase(checkResult) {
        case Left(AuthError.StolenSession) => invalidateSession(sessionId)
      }

    } yield checkResult

  override def invalidateSession(sessionId: SessionId): Task[Unit] =
    sessionService.invalidateSession(sessionId)

  override def checkSession(sessionId: SessionId): Task[Either[AuthError, Unit]] =
    for {
      sessionOpt <- sessionService.findSession(sessionId)
      checkResult = sessionOpt match {
        case Some(session) =>
          Either.cond(session.exp.isAfter(Instant.now()), () , AuthError.ExpiredSession)
        case None =>
          Left(AuthError.InvalidSession)
      }
    } yield checkResult
}
