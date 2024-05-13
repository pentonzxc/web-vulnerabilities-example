import akka.http.scaladsl.model.{HttpHeader, MediaType}
import akka.http.scaladsl.model.headers.{Accept, ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.http.scaladsl.server.{Directive0, Directive1}
import akka.http.scaladsl.server.Directives.{deleteCookie, headerValue, onSuccess}
import model.{Login, SecretToken, SessionId}

import scala.util.{Success, Try}

package object controller {

  case class `X-CSRF-TOKEN`(token: SecretToken) extends ModeledCustomHeader[`X-CSRF-TOKEN`] {
    override def companion: ModeledCustomHeaderCompanion[`X-CSRF-TOKEN`] = `X-CSRF-TOKEN`

    override def value(): String = token.value

    override def renderInRequests(): Boolean = true

    override def renderInResponses(): Boolean = false
  }


  object `X-CSRF-TOKEN` extends ModeledCustomHeaderCompanion[`X-CSRF-TOKEN`] {
    override def name: String = "X-CSRF-TOKEN"

    override def parse(value: String): Try[`X-CSRF-TOKEN`] =
      Success(`X-CSRF-TOKEN`(SecretToken(value)))
  }

  def acceptMediaTypes(mediaTypes: MediaType*): Directive1[MediaType] = {
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

  val deleteSessionCookie: Directive0 = deleteCookie("session")
}
