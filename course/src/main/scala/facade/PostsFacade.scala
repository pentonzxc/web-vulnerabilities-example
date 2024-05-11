package facade

import dto.post.PostDto
import model.{Generator, Login, Post, PostId, UserId}
import service.{PostsService, UserService}
import zio.Task

trait PostsFacade {
  def findByLogin(login : Login) : Task[List[Post]]
  def create(post : PostDto) : Task[Unit]
  def delete(postId : PostId) : Task[Unit]
}


class PostsFacadeImpl(postService : PostsService) extends PostsFacade {
  override def findByLogin(login: Login): Task[List[Post]] =
    postService.findPostsByLogin(login)

  override def create(post: PostDto): Task[Unit] = {
    postService.create(post.login , content = post.content)
  }

  override def delete(postId: PostId): Task[Unit] = {
    postService.delete(postId)
  }
}
