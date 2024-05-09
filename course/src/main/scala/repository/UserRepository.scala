package repository

import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import doobie.{Meta, Read}
import model.{Login, User, UserId}
import zio.Task
import zio.interop.catz._

import java.util.UUID

trait UserRepository {
  def findAuthUser(userId: UserId): Task[Option[User]]
  def findByLogin(login: Login): Task[Option[User]]
  def createIfNotExists(user: User): Task[Unit]
}

class PostgresUserRepository(tx: Transactor[Task]) extends UserRepository with QueryImplicits {
  override def findAuthUser(userId: UserId): Task[Option[User]] =
    sql"SELECT id, login, password FROM users WHERE id = $userId"
      .query[User]
      .option
      .transact(tx)


//  TODO: implement right
  override def createIfNotExists(user: User): Task[Unit] =
    sql"INSERT INTO users (id, login , password) VALUES (${user.id} , ${user.login} , ${user.password})"
      .update
      .run
      .transact(tx)
      .unit

  override def findByLogin(login: Login): Task[Option[User]] =
    sql"SELECT id, login, password FROM users WHERE login = $login"
      .query[User]
      .option
      .transact(tx)

  private implicit val authUserRead: Read[User] = Read[(UserId, Login, String)]
    .map(t => User(t._1, t._2, t._3))

}
