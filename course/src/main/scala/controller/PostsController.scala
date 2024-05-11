package controller

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`, `must-revalidate`, `no-cache`, `no-store`}
import akka.http.scaladsl.model.headers.{Accept, CacheDirective, `Cache-Control`}
import akka.http.scaladsl.model.{HttpHeader, MediaType, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.post._
import facade.{PostsFacade, SessionFacade}
import io.circe.Encoder._
import io.circe.syntax._
import model.error.{AuthError, InvalidUserException}
import model.{Login, SessionId}
import utils.ZIOFutures._

class PostsController(postsFacade: PostsFacade, sessionFacade: SessionFacade) extends Controller {

  private val withSessionCookie = cookie("session")
  private val deleteSessionCookie = deleteCookie("session")
  private val LoginSegment = Segment.map(Login(_)) ~ (PathEnd | Slash)

  private val createPost =
    path("posts" / LoginSegment) { login =>
      post {
        withSessionCookie { session =>
          onSuccess(sessionFacade.checkSessionWithOwner(SessionId(session.value), login).unsafeToFuture) {
            checkedSessionResult =>
              entity(as[CreatePostRequest]) { createPostRequest =>
                val postDto = PostDto(content = createPostRequest.content, login = login)

                checkedSessionResult match {
                  case Right(_) =>
                    onSuccess(postsFacade.create(postDto).unsafeToFuture) {
                      complete(StatusCodes.Created)
                    }

                  // FIXME: server doens't send cookie invalidation
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
    }

  private val getPosts =
    path("posts" / LoginSegment) { login =>
      (get & acceptMediaTypes(MediaTypes.`text/html`, MediaTypes.`application/json`)) { mediaType =>
        respondWithHeaders(noCacheHeader, contentTypeSecurityPolicyHeader)   {
          withSessionCookie { session =>
            if (MediaTypes.`text/html`.matches(mediaType)) {
              getFromResource("static/posts.html")
            } else {
              onSuccess(sessionFacade.checkSession(SessionId(session.value)).unsafeToFuture) {
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
        }
      }
    }

  def acceptMediaTypes(mediaTypes: MediaType*) = {
    def isSupported(header: HttpHeader) = {
      header match {
        case accept: Accept => mediaTypes.collectFirst {
            case typ if accept.mediaRanges.exists(_.matches(typ)) => typ
          }
        case _ => None
      }
    }

//    wtf
    headerValue(isSupported(_))
  }

  val noCacheHeader = `Cache-Control`(`no-cache`, `must-revalidate`, `no-store`, `max-age`(0))
  val contentTypeSecurityPolicyHeader = HttpHeader.parse("Content-Security-Policy", "script-src 'nonce-rAnd0m'; default-src 'self'") match {
    case ParsingResult.Ok(header, errors) => header
    case ParsingResult.Error(error) => throw new RuntimeException("Can't parse `Content-Security-Policy` header")
  }


  override def route: Route =
    handleInvalidUserException {
      concat(
        getPosts,
        createPost
      )
    }

  private def handleInvalidUserException =
    handleExceptions {
      ExceptionHandler {
        case _: InvalidUserException => complete(StatusCodes.BadRequest, "invalid_user")
      }
    }
}
