package dto

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import model.{Login, UserId}

object post {
  case class PostDto(content : String, login : Login)
  case class CreatePostRequest(content : String) extends AnyVal


  implicit val createPostRequestJsonDecoder : Decoder[CreatePostRequest] = deriveDecoder
}
