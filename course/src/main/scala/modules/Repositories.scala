package modules

import doobie.util.transactor.Transactor
import repository.{InMemorySessionRepository, PostgresUserRepository, SessionRepository, UserRepository}
import zio.Task

class Repositories(postgresTx : Transactor[Task]) {
  val userRepository : UserRepository = new PostgresUserRepository(postgresTx)
  val sessionRepository : SessionRepository = new InMemorySessionRepository()
}
