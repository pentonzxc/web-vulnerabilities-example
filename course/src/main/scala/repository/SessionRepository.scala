package repository

import model.SessionId
import model.auth.Session
import zio.{Task, ZIO}

import scala.collection.concurrent.TrieMap

trait SessionRepository {
  def find(sessionId: SessionId): Task[Option[Session]]
  def create(session: Session): Task[Unit]
  def delete(sessionId: SessionId) : Task[Unit]
}

class InMemorySessionRepository extends SessionRepository {
  private val sessions: TrieMap[SessionId, Session] = TrieMap.empty

  override def find(sessionId: SessionId): Task[Option[Session]] =
    ZIO.attempt(sessions.get(sessionId))

  override def create(session: Session): Task[Unit] =
    ZIO.attempt(sessions.put(session.id, session))

  override def delete(sessionId: SessionId): Task[Unit] =
    ZIO.attempt(sessions.remove(sessionId)).unit
}
