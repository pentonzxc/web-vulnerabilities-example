package model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class User(id: UserId, login: String, password: String)

object User {
  implicit val authUserEncoder : Encoder[User] = deriveEncoder
  implicit val authUserDecoder : Decoder[User] = deriveDecoder
}