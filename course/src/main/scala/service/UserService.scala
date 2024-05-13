package service

import dto.AuthUser
import model.error.ApiError
import model.{Generator, Login, User, UserId}
import repository.UserRepository
import zio.Task

import java.util.UUID

trait UserService {
  def authenticate(login: Login, password: String): Task[Either[ApiError, UserId]]
  def findUserByLogin(login: Login): Task[Option[User]]
  def create(authUser: AuthUser): Task[Unit]
}

class UserServiceImpl(userRepository: UserRepository) extends UserService {

  override def authenticate(login: Login, password: String): Task[Either[ApiError, UserId]] =
    userRepository.findByLogin(login).map { userOpt =>
      for {
        user <- userOpt.toRight(ApiError.InvalidUser)
        _ <- Either.cond(user.password == password, (), ApiError.InvalidPassword)
      } yield user.id
    }

  override def create(authUser: AuthUser): Task[Unit] =
    userRepository.createIfNotExists(newUser(authUser))

  private def newUser(authUser: AuthUser): User =
    User(id = Generator[UserId].next(), login = authUser.login, password = authUser.password)

  override def findUserByLogin(login: Login): Task[Option[User]] =
    userRepository.findByLogin(login)
}
