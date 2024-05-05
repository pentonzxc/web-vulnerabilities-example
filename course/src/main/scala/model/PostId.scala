package model

import java.util.UUID

case class PostId (value : UUID) extends AnyVal


object PostId {
  implicit val postIdGen : Generator[PostId] = Generator.from[UUID, PostId](PostId(_))
}