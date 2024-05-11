package controller

import akka.event.Logging.LogLevel
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, RouteResult}

object AkkaLogging {
  private def akkaResponseTimeLoggingFunction(
      loggingAdapter: LoggingAdapter,
      requestTimestamp: Long,
      level: LogLevel = Logging.InfoLevel)(req: HttpRequest)(res: RouteResult): Unit = {
    val entry = res match {
      case Complete(resp) =>
        val responseTimestamp: Long = System.nanoTime
        val elapsedTime: Long = (responseTimestamp - requestTimestamp) / 1000000
        val loggingString = s"""Request: method - ${req.method.value}, url - ${req.uri}, response status - ${resp.status}, elapsed time - $elapsedTime ms"""
        LogEntry(loggingString, level)
      case Rejected(reason) =>

        LogEntry(s"Rejected Reason: ${reason.mkString(",")}", level)
    }
    entry.logTo(loggingAdapter)
  }

  private def printResponseTime(log: LoggingAdapter) = {
    val requestTimestamp = System.nanoTime
    akkaResponseTimeLoggingFunction(log, requestTimestamp) _
  }

  val logDirective: Directive0 = DebuggingDirectives.logRequestResult(LoggingMagnet(printResponseTime))
}
