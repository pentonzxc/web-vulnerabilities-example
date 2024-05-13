package repository

import cats.effect.Sync
import doobie.Read
import doobie.free.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import model.error.UserAlreadyExistException
import model.{Login, User, UserId}
import zio.Task
import zio.interop.catz._

trait UserRepository {
  def find(userId: UserId): Task[Option[User]]
  def findByLogin(login: Login): Task[Option[User]]
  def createIfNotExists(user: User): Task[Unit]
}

class PostgresUserRepository(tx: Transactor[Task]) extends UserRepository with QueryImplicits {
  override def find(userId: UserId): Task[Option[User]] =
    findQuery(userId).transact(tx)

  private def findQuery(userId: UserId) =
    sql"SELECT id, login, password FROM users WHERE id = $userId"
      .query[User]
      .option


  private def findByLoginQuery(login : Login) =
    sql"SELECT id, login, password FROM users WHERE login = $login"
      .query[User]
      .option

//  TODO: implement right
  override def createIfNotExists(user: User): Task[Unit] = {
    val createUser =
      sql"INSERT INTO users (id, login , password) VALUES (${user.id} , ${user.login} , ${user.password})"
        .update
        .run

    val transaction = for {
      userOpt <- findByLoginQuery(user.login)
      _ <- userOpt match {
        case Some(_) => Sync[ConnectionIO].raiseError(new UserAlreadyExistException)
        case None => createUser.map(_ => ())
      }
    } yield ()

    transaction
      .transact(tx)
  }

  override def findByLogin(login: Login): Task[Option[User]] =
    sql"SELECT id, login, password FROM users WHERE login = $login"
      .query[User]
      .option
      .transact(tx)

  private implicit val authUserRead: Read[User] = Read[(UserId, Login, String)]
    .map(t => User(t._1, t._2, t._3))

}
