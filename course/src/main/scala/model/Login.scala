package model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class Login (value : String) extends AnyVal


object Login {
  implicit val authUserDecoder: Decoder[Login] = Decoder[String].map(Login(_))
  implicit val authUserEncoder: Encoder[Login] = Encoder[String].contramap(_.value)
}
