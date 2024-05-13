package model.error

sealed abstract class ApiError(val message : String) extends Product


object ApiError {
  final case object InvalidUser extends ApiError("invalid_user")
  final case object InvalidPassword extends ApiError("invalid_password")
  final case object UserAlreadyExist extends ApiError("user_already_exist")
  final case object ExpiredSession extends ApiError("expired_session")
  final case object InvalidSession extends ApiError("invalid_session")
  final case object StolenSession extends ApiError("stolen_session")
  final case object InvalidCsrf extends ApiError("invalid_csrf")
}
