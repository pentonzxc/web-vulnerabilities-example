package controller

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`, `must-revalidate`, `no-cache`, `no-store`}
import akka.http.scaladsl.model.headers.{Accept, `Cache-Control`}
import akka.http.scaladsl.model.{HttpHeader, MediaType, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.post._
import facade.{PostsFacade, SessionFacade}
import io.circe.Encoder._
import io.circe.syntax._
import model.error.{AuthError, InvalidUserException}
import model.{Login, PostId, SessionId}
import utils.ZIOFutures._

class PostsController(postsFacade: PostsFacade, sessionFacade: SessionFacade) extends Controller {

  private val withSessionCookie: Directive1[SessionId] = cookie("session").map(c => SessionId(c.value))
  private val deleteSessionCookie = deleteCookie("session")
  private val LoginMatcher = Segment.map(Login(_))
  private val PostIdMatcher = JavaUUID.map(PostId(_))

  private val NoCacheHeader = `Cache-Control`(`no-cache`, `must-revalidate`, `no-store`, `max-age`(0))
  private val ContentTypeSecurityPolicyHeader =
    HttpHeader.parse("Content-Security-Policy", "script-src 'nonce-rAnd0m'; default-src 'self'") match {
      case ParsingResult.Ok(header, _) => header
      case ParsingResult.Error(_) => throw new RuntimeException("Can't parse `Content-Security-Policy` header")
    }

  private val createPost =
    path("posts" / LoginMatcher) { login =>
      post {
        withSessionCookie { session =>
          checkSessionDirective(session, login) {
            entity(as[CreatePostRequest]) { createPostRequest =>
              val postDto = PostDto(content = createPostRequest.content, login = login)
              onSuccess(postsFacade.create(postDto).unsafeToFuture) {
                complete(StatusCodes.Created)
              }
            }
          }
        }
      }
    }

  private val getPosts =
    path("posts" / LoginMatcher) { login =>
      (get & acceptMediaTypes(MediaTypes.`text/html`, MediaTypes.`application/json`)) { mediaType =>
        respondWithHeaders(NoCacheHeader, ContentTypeSecurityPolicyHeader) {
          if (MediaTypes.`application/json`.matches(mediaType)) {
            onSuccess(postsFacade.findByLogin(login).unsafeToFuture) { posts =>
              complete(posts.asJson)
            }
          } else {
            getFromResource("static/posts.html")
          }
        }
      }
    }


  private val deletePosts =
    path("posts" / LoginMatcher /  PostIdMatcher) { (login , postId) =>
      delete {
        withSessionCookie { session =>
          checkSessionDirective(session , login) {
            onSuccess(postsFacade.delete(postId).unsafeToFuture) {
              complete(StatusCodes.OK)
            }
          }
        }
      }
    }

  override def route: Route =
    handleInvalidUserException {
      concat(
        getPosts,
        createPost
      )
    }

  private def checkSessionDirective(
      sessionId: SessionId,
      login: Login): Directive0 = {
    onSuccess(
      sessionFacade.checkSession(sessionId = sessionId, maybeSessionOwner = login).unsafeToFuture
    ).flatMap {
      case Left(authError) => authError match {
          // add cookie invalidation
          case AuthError.ExpiredSession =>
            complete(StatusCodes.Forbidden, "session_is_expired")
          case AuthError.InvalidSession =>
            complete(StatusCodes.Forbidden, "invalid_session")
          case AuthError.StolenSession =>
            complete(StatusCodes.Forbidden, "session_is_stolen")
          case _ =>
            complete(StatusCodes.InternalServerError)
        }
      case Right(_) => pass
    }
  }

  private def acceptMediaTypes(mediaTypes: MediaType*) = {
    def isSupported(header: HttpHeader) = {
      header match {
        case accept: Accept => mediaTypes.collectFirst {
            case typ if accept.mediaRanges.exists(_.matches(typ)) => typ
          }
        case _ => None
      }
    }

    headerValue(isSupported(_))
  }

  private def handleInvalidUserException =
    handleExceptions {
      ExceptionHandler {
        case _: InvalidUserException => complete(StatusCodes.BadRequest, "invalid_user")
      }
    }
}
