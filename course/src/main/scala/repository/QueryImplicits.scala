package repository

import doobie.Meta
import model.{Login, PostId, UserId}
import doobie.postgres.implicits._

import java.util.UUID

trait QueryImplicits {
  implicit val userIdMeta : Meta[UserId] = Meta[UUID].timap(UserId(_))(_.value)
  implicit val postIdMeta : Meta[PostId] = Meta[UUID].timap(PostId(_))(_.value)
  implicit val loginMeta : Meta[Login] = Meta[String].timap(Login(_))(_.value)
}
