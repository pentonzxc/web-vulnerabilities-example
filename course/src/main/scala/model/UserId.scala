package model

import io.circe.{Decoder, Encoder}

import java.util.UUID

case class UserId(value : UUID) extends AnyVal

object UserId {
  implicit val postIdGen : Generator[UserId] = Generator.from(UserId(_))
  implicit val userIdEncoder : Encoder[UserId] = Encoder[UUID].contramap(_.value)
  implicit val userIdDecoder : Decoder[UserId] = Decoder[UUID].map(UserId(_))
}

