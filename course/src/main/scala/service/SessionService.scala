package service

import model.{Generator, SessionId, UserId}
import model.auth.Session
import repository.SessionRepository
import zio.Task

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

trait SessionService {
  def issueSession(userId: UserId, createdAt: Instant, ttl: FiniteDuration) : Task[Session]
  def findSession(sessionId : SessionId) : Task[Option[Session]]
  def invalidateSession(sessionId : SessionId) : Task[Unit]
}


class SessionServiceImpl(sessionRepository : SessionRepository) extends SessionService {
  override def issueSession(userId: UserId, createdAt: Instant, ttl: FiniteDuration): Task[Session] = {
    val expireAt = createdAt.plusNanos(ttl.toNanos)
    val session = Session(id = Generator[SessionId].next() , iss = userId,  created = createdAt, exp = expireAt)

    sessionRepository.create(session).as(session)
  }

  override def findSession(sessionId: SessionId): Task[Option[Session]] = {
    sessionRepository.find(sessionId)
  }

  override def invalidateSession(sessionId: SessionId): Task[Unit] =
    sessionRepository.delete(sessionId)
}

