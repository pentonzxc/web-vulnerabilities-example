package model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import model.UserId._
import model.PostId._

case class Post(id: PostId, content: String, userId: UserId)

object Post {
  implicit val postJsonEncoder: Encoder[Post] = deriveEncoder
  implicit val postJsonDecoder: Decoder[Post] = deriveDecoder
  implicit val generator: Generator[PostId] = Generator.from(PostId(_))
}
