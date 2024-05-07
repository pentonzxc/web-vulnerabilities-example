package model

import java.util.concurrent.ThreadLocalRandom

case class SessionId(value: String) extends AnyVal

object SessionId {
  implicit val sessionIdGen : Generator[SessionId] = () =>
    SessionId("JSESSION" + String.valueOf(ThreadLocalRandom.current().nextLong()))
}
