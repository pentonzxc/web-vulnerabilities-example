package dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import model.Login
import model.Login._

case class AuthUser(login : Login, password : String)


object AuthUser {
  implicit val authUserDecoder: Decoder[AuthUser] = deriveDecoder
  implicit val authUserEncoder: Encoder[AuthUser] = deriveEncoder
}
