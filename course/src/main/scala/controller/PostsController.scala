package controller

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`, `must-revalidate`, `no-cache`, `no-store`}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.model.{HttpHeader, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.post._
import facade.{PostsFacade, SessionFacade}
import io.circe.Encoder._
import io.circe.syntax._
import model.error.InvalidUserException
import model.{Login, PostId, SessionId}
import utils.ZIOFutures._

class PostsController(postsFacade: PostsFacade, sessionFacade: SessionFacade, authDirectives: AuthDirectives)
  extends Controller {

  private val withSessionCookie: Directive1[SessionId] = optionalCookie("session")
    .flatMap {
      case Some(c) => provide(SessionId(c.value))
      case None => complete(StatusCodes.BadRequest, "invalid_session")
    }
  private val LoginMatcher = Segment.map(Login(_))
  private val PostIdMatcher = JavaUUID.map(PostId(_))

  private val NoCacheHeader = `Cache-Control`(`no-cache`, `must-revalidate`, `no-store`, `max-age`(0))
  private val ContentTypeSecurityPolicyHeader =
//    HttpHeader.parse("Content-Security-Policy", "script-src 'unsafe-inline' 'nonce-rAnd0m'; default-src 'self'") match {
    HttpHeader.parse("Content-Security-Policy", "script-src 'unsafe-inline'; default-src 'self'") match {
      case ParsingResult.Ok(header, _) => header
      case ParsingResult.Error(_) => throw new RuntimeException("Can't parse `Content-Security-Policy` header")
    }

  private val createPost =
    path("posts" / LoginMatcher) { login =>
      post {
        (withSessionCookie & headerValueByType[`X-CSRF-TOKEN`]()) { case (session, csrfToken) =>
          authDirectives.checkSessionDirective(session, csrfToken.token, login, invalidateSessionCookie = true) {
            entity(as[CreatePostRequest]) { createPostRequest =>
              val postDto = PostDto(content = createPostRequest.content, login = login)
              onSuccess(postsFacade.create(postDto).unsafeToFuture) {
                respondWithHeader(csrfToken) {
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
        respondWithHeaders(NoCacheHeader, ContentTypeSecurityPolicyHeader) {
          getFromResource("static/posts.html")
        }
      }
    }

  private val getPosts =
    path("posts" / LoginMatcher) { login =>
      (get & acceptMediaTypes(MediaTypes.`application/json`)) { _ =>
        respondWithHeaders(NoCacheHeader, ContentTypeSecurityPolicyHeader) {
          onSuccess(postsFacade.findByLogin(login).unsafeToFuture) { posts =>
            complete(posts.asJson)
          }
        }
      }
    }

  private val deletePost =
    path("posts" / LoginMatcher / PostIdMatcher) { case (login , postId) =>
      delete {
        (withSessionCookie & headerValueByType[`X-CSRF-TOKEN`]()) { case (sessionCookie, csrfToken) =>
          authDirectives.checkSessionDirective(sessionCookie, csrfToken.token, login, invalidateSessionCookie = true) {
            onSuccess(postsFacade.delete(postId).unsafeToFuture) {
              respondWithHeader(csrfToken) {
                complete(StatusCodes.NoContent)
              }
            }
          }
        }
      }
    }

  override def route: Route =
    handleInvalidUserException {
      concat(
        getPostsPage,
        getPosts,
        createPost,
        deletePost
      )
    }

  private def handleInvalidUserException =
    handleExceptions {
      ExceptionHandler {
        case _: InvalidUserException => complete(StatusCodes.BadRequest, "invalid_user")
      }
    }
}
