import akka.http.scaladsl.model.headers.{Accept, HttpCookie, ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.http.scaladsl.model.{DateTime, HttpHeader, MediaType}
import akka.http.scaladsl.server.Directives.{deleteCookie, headerValue, optionalCookie}
import akka.http.scaladsl.server.{Directive, Directive0, Directive1}
import model.{SecretToken, SessionId}

import java.security.SecureRandom
import java.time.Instant
import scala.util.{Success, Try}

package object controller {

  val sessionCookieOpt: Directive[Tuple1[Option[SessionId]]] = optionalCookie("session").map(_.map(c => SessionId(c.value)))
  val removeSessionCookie: Directive0 = deleteCookie("session")

  def sessionCookie(session: SessionId, exp: Instant): HttpCookie =
    HttpCookie(name = "session", value = session.value, expires = Some(DateTime.apply(exp.toEpochMilli)))


  case class `X-CSRF-TOKEN`(token: SecretToken) extends ModeledCustomHeader[`X-CSRF-TOKEN`] {
    override def companion: ModeledCustomHeaderCompanion[`X-CSRF-TOKEN`] = `X-CSRF-TOKEN`

    override def value(): String = token.value

    override def renderInRequests(): Boolean = true

    override def renderInResponses(): Boolean = true
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


  private val threadLocalRandom: ThreadLocal[SecureRandom] = ThreadLocal.withInitial(() => new SecureRandom())
  def generateCsrfToken(): SecretToken = {
    val random = threadLocalRandom.get()
    val buffer = new StringBuilder
    val length = Alphanumeric.length

    (0 until 32).foreach { _ =>
      buffer append Alphanumeric.charAt(random.nextInt(length)).toString
    }

    SecretToken(buffer.toString())
  }

  private val Alphanumeric: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
}
