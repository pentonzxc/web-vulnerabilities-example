package controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.post._
import facade.{PostsFacade, SessionFacade}
import io.circe.Encoder._
import io.circe.syntax._
import model.error.AuthError
import model.{Login, SessionId}
import utils.ZIOFutures._

class PostsController(postsFacade: PostsFacade, sessionService: SessionFacade) extends Controller {

  private val createPost = (pathPrefix("posts" / Segment) & post) { rawLogin =>
    withSession { session =>
      val login = Login(rawLogin)
      val checkedSession = sessionService.checkSessionWithOwner(SessionId(session.value), login)

      onSuccess(checkedSession.unsafeToFuture) { checkedSessionResult =>
        entity(as[CreatePostRequest]) { createPostRequest =>
          val postDto = PostDto(content = createPostRequest.content, login = login)

          checkedSessionResult match {
            case Right(_) =>
              onSuccess(postsFacade.create(postDto).unsafeToFuture) {
                complete(StatusCodes.Created)
              }

            case Left(authError) =>
              authError match {
                case AuthError.ExpiredSession =>
                  deleteSessionCookie {
                    complete(StatusCodes.Forbidden, "session_is_expired")
                  }
                case AuthError.InvalidSession =>
                  deleteSessionCookie {
                    complete(StatusCodes.Forbidden, "invalid_session")
                  }
                case AuthError.StolenSession =>
                  deleteSessionCookie {
                    complete(StatusCodes.Forbidden, "session_is_stolen")
                  }
                case _ =>
                  complete(StatusCodes.InternalServerError)
              }
          }
        }
      }
    }
  }

  private val getPosts = (pathPrefix("/posts" / Segment) & get) { rawLogin =>
    withSession { session =>
      val login = Login(rawLogin)
      val checkedSession = sessionService.checkSession(SessionId(session.value))

      onSuccess(checkedSession.unsafeToFuture) {
        case Right(_) =>
          onSuccess(postsFacade.findByLogin(login).unsafeToFuture) { posts =>
            complete(posts.asJson)
          }

        case Left(authError) =>
          authError match {
            case AuthError.ExpiredSession => complete(StatusCodes.Forbidden, "session_is_expired")
            case AuthError.InvalidSession => complete(StatusCodes.Forbidden, "invalid_session")
            case _ => complete(StatusCodes.InternalServerError)
          }
      }

    }
  }

  // TODO: set up cookie: samesite, domain and etc)
  private val withSession = cookie("session")

  private val deleteSessionCookie = deleteCookie("session")

  override def route: Route =
    concat(
      getPosts,
      createPost
    )

}
