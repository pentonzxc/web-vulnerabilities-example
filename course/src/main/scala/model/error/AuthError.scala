package model.error

sealed abstract class AuthError(val message : String) extends Product

sealed abstract class SessionError(message : String) extends AuthError(message)


object AuthError {
  final case object InvalidUser extends AuthError("invalid_user")
  final case object InvalidPassword extends AuthError("invalid_password")
  final case object UserAlreadyExist extends AuthError("invalid_user")
  final case object ExpiredSession extends SessionError("expired_session")
  final case object InvalidSession extends SessionError("invalid_session")
  final case object StolenSession extends SessionError("stolen_session")
  final case object InvalidCsrf extends AuthError("invalid_csrf")
}
