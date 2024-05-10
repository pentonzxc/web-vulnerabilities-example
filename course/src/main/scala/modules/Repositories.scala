package modules

import doobie.util.transactor.Transactor
import repository.{InMemorySessionRepository, PostgresPostsRepository, PostgresUserRepository, PostsRepository, SessionRepository, UserRepository}
import zio.Task

class Repositories(postgresTx : Transactor[Task]) {
  val userRepository : UserRepository = new PostgresUserRepository(postgresTx)
  val postsRepository : PostsRepository = new PostgresPostsRepository(postgresTx)
  val sessionRepository : SessionRepository = new InMemorySessionRepository()
}
