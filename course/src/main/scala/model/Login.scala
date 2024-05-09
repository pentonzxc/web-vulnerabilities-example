package model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class Login (value : String) extends AnyVal


object Login {
  implicit val authUserDecoder: Decoder[Login] = deriveDecoder
  implicit val authUserEncoder: Encoder[Login] = deriveEncoder
}
