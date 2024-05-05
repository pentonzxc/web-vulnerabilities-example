package model.error

// make enum
sealed trait AuthError

object AuthError {
  final case object NonExistUser extends AuthError
  final case object InvalidPassword extends AuthError
  final case object UserAlreadyExist extends AuthError
}
