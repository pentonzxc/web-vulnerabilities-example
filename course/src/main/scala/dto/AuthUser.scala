package dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class AuthUser(login : String, password : String)


object AuthUser {
  implicit val authUserDecoder: Decoder[AuthUser] = deriveDecoder
  implicit val authUserEncoder: Encoder[AuthUser] = deriveEncoder
}
