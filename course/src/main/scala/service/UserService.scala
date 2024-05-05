package service

import dto.AuthUser
import model.error.AuthError
import model.{User, UserId}
import repository.UserRepository
import zio.{IO, ZIO}

import java.util.UUID

trait UserService {
  def authenticate(login: String, password: String): IO[AuthError, UserId]
  def register(authUser: AuthUser): IO[AuthError, Unit]
}

class UserServiceImpl(userRepository: UserRepository) extends UserService {

  override def authenticate(login: String, password: String): IO[AuthError, UserId] = {
    val checkPassword = (p: String) =>
      if (theSamePasswords(p, password)) ZIO.unit
      else ZIO.fail(AuthError.InvalidPassword)

    for {
      userOpt <- userRepository.findByLogin(login).orDie
      user <- userOpt match {
        case Some(user) => ZIO.succeed(user)
        case None => ZIO.fail(AuthError.NonExistUser)
      }
      _ <- checkPassword(user.password)
    } yield user.id
  }

  override def register(authUser: AuthUser): IO[AuthError, Unit] =
    userRepository.createIfNotExists(userFromAuthUser(authUser)).orDie

  private def theSamePasswords(pass1: String, pass2: String) =
    pass1 == pass2

  private def userFromAuthUser(authUser: AuthUser): User =
    User(id = UserId(UUID.randomUUID()), login = authUser.login, password = authUser.password)

}
