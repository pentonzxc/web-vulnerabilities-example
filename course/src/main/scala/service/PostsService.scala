package service

import model.{Generator, Login, Post, PostId, UserId}
import repository.PostsRepository
import zio.Task

trait PostsService {
  def findPosts(userId : UserId) : Task[List[Post]]
  def findPostsByLogin(login : Login) : Task[List[Post]]
  def create(login : Login , content : String) : Task[Unit]
  def delete(postId: PostId) : Task[Unit]
}


class PostsServiceImpl(userService : UserService, postsRepository: PostsRepository) extends PostsService {
  override def findPosts(userId : UserId): Task[List[Post]] =
    postsRepository.findPosts(userId)


  override def create(login : Login , content : String): Task[Unit] =
    for {
      userOpt <- userService.findUserByLogin(login)

      userId = userOpt.map(_.id).getOrElse(throw new RuntimeException("user non exist"))
      newPost = Post(Generator[PostId].next(),  content = content , userId = userId)

      _ <- postsRepository.create(newPost)
    } yield ()


  override def findPostsByLogin(login : Login) : Task[List[Post]] =
    for {
      userOpt <- userService.findUserByLogin(login)

      userId = userOpt.map(_.id).getOrElse(throw new RuntimeException("user non exist"))

      posts <- postsRepository.findPosts(userId)
    } yield posts

  override def delete(postId: PostId): Task[Unit] =
    postsRepository.delete(postId)

}
