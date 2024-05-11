package service

import dto.AuthUser
import model.error.AuthError
import model.{Login, User, UserId}
import repository.UserRepository
import zio.Task

import java.util.UUID

trait UserService {
  def authenticate(login: Login, password: String): Task[Either[AuthError, UserId]]

  def findUserByLogin(login: Login): Task[Option[User]]

  def create(authUser: AuthUser): Task[Unit]
}

class UserServiceImpl(userRepository: UserRepository) extends UserService {

  override def authenticate(login: Login, password: String): Task[Either[AuthError, UserId]] =
    userRepository.findByLogin(login).map { userOpt =>
      for {
        user <- userOpt.toRight(AuthError.InvalidUser)
        _ <- Either.cond(user.password == password, (), AuthError.InvalidPassword)
      } yield user.id
    }

  override def create(authUser: AuthUser): Task[Unit] =
    userRepository.createIfNotExists(userFromAuthUser(authUser))

  private def userFromAuthUser(authUser: AuthUser): User =
    User(id = UserId(UUID.randomUUID()), login = authUser.login, password = authUser.password)

  override def findUserByLogin(login: Login): Task[Option[User]] =
    userRepository.findByLogin(login)
}
