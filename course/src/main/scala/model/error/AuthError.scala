package model.error

// make ApiError and extends AuthError from it
sealed trait AuthError extends Product

object AuthError {
  final case object InvalidUser extends AuthError
  final case object InvalidPassword extends AuthError
  final case object UserAlreadyExist extends AuthError
  final case object ExpiredSession extends AuthError
  final case object InvalidSession extends AuthError
  final case object StolenSession extends AuthError
}
