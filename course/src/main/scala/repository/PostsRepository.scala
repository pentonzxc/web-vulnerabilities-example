package repository

import doobie.implicits._
import doobie.util.transactor.Transactor
import model.{Login, Post, PostId, UserId}
import zio.Task
import zio.interop.catz._

trait PostsRepository {
  def findPosts(owner: UserId): Task[List[Post]]
  def create(post: Post): Task[Unit]
  def delete(postId : PostId) : Task[Unit]
}

class PostgresPostsRepository(tx: Transactor[Task]) extends PostsRepository with QueryImplicits {
  def findPosts(owner: UserId): Task[List[Post]] =
    sql"SELECT id, content, user_id FROM posts WHERE user_id = $owner"
      .query[Post]
      .to[List]
      .transact(tx)

  override def create(post: Post): Task[Unit] = {
    import post._
    sql"INSERT INTO posts (id, content, user_id) VALUES ($id, $content , $userId)"
      .update
      .run
      .transact(tx)
      .unit
  }

  override def delete(postId: PostId): Task[Unit] =
    sql"DELETE FROM posts WHERE id = $postId"
      .update
      .run
      .transact(tx)
      .unit
}
