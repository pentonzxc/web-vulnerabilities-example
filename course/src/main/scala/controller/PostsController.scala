package controller

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{HttpHeader, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.post._
import facade.{PostsFacade, SessionFacade}
import io.circe.Encoder._
import io.circe.syntax._
import model.error.{ApiError, InvalidUserException}
import utils.ZIOFutures._

class PostsController(postsFacade: PostsFacade, sessionFacade: SessionFacade, authDirectives: AuthDirectives)
  extends Controller {

  private val cspHeader =
//    HttpHeader.parse("Content-Security-Policy", "script-src 'nonce-rAnd0m'; default-src 'self'") match {
    HttpHeader.parse("Content-Security-Policy", "script-src 'unsafe-inline'; default-src 'self'") match {
      case ParsingResult.Ok(header, _) => header
      case ParsingResult.Error(_) => throw new RuntimeException("Can't parse `Content-Security-Policy` header")
    }

  private val createPost =
    path("posts" / LoginMatcher) { login =>
      post {
        (provideSessionCookie & provideCsrfToken) { case (session, csrfToken) =>
          authDirectives.checkSessionDirective(session, csrfToken, login, invalidateSessionCookie = true) {
            entity(as[CreatePostRequest]) { createPostRequest =>
              val postDto = PostDto(content = createPostRequest.content, login = login)
              onSuccess(postsFacade.create(postDto).unsafeToFuture) {
                respondWithHeader(`X-CSRF-TOKEN`(csrfToken)) {
                  complete(StatusCodes.Created)
                }
              }
            }
          }
        }
      }
    }

  private val getPostsPage =
    path("posts" / LoginMatcher) { _ =>
      (get & acceptMediaTypes(MediaTypes.`text/html`)) { _ =>
        respondWithHeaders(NoCacheHeader, cspHeader) {
          getFromResource("static/posts.html")
        }
      }
    }

  private val getPosts =
    path("posts" / LoginMatcher) { login =>
      (get & acceptMediaTypes(MediaTypes.`application/json`)) { _ =>
          onSuccess(postsFacade.findByLogin(login).unsafeToFuture) { posts =>
            complete(posts.asJson)
          }
        }
    }

  private val deletePost =
    path("posts" / LoginMatcher / PostIdMatcher) { case (login, postId) =>
      delete {
        (provideSessionCookie & provideCsrfToken) { case (sessionCookie, csrfToken) =>
          authDirectives.checkSessionDirective(sessionCookie, csrfToken, login, invalidateSessionCookie = true) {
            onSuccess(postsFacade.delete(postId).unsafeToFuture) {
              respondWithHeader(`X-CSRF-TOKEN`(csrfToken)) {
                complete(StatusCodes.NoContent)
              }
            }
          }
        }
      }
    }

  override def route: Route =
    handleInvalidUserException {
      handleAbsentAuthenticationFacts {
        concat(
          getPostsPage,
          getPosts,
          createPost,
          deletePost
        )
      }
    }

  private def handleInvalidUserException =
    handleExceptions {
      ExceptionHandler {
        case _: InvalidUserException => complete(StatusCodes.BadRequest, "invalid_user")
      }
    }

  private def handleAbsentAuthenticationFacts =
    handleRejections {
      RejectionHandler.newBuilder().handle {
        case MissingCookieRejection("session") => complete(StatusCodes.BadRequest, ApiError.InvalidSession.message)
        case MissingHeaderRejection("X-CSRF-TOKEN") => complete(StatusCodes.BadRequest, ApiError.InvalidCsrf.message)
      }.result()
    }
}
