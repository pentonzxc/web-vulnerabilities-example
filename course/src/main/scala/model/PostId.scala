package model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.UUID

case class PostId (value : UUID) extends AnyVal


object PostId {
  implicit val postIdJsonEncoder: Encoder[PostId] = Encoder[UUID].contramap(_.value)
  implicit val postIdJsonDecoder : Decoder[PostId] = Decoder[UUID].map(PostId(_))
  implicit val postIdGen : Generator[PostId] = Generator.from[UUID, PostId](PostId(_))
}