package utils

import zio.{Unsafe, ZIO, Runtime}

import scala.concurrent.Future

object ZIOFutures {
  implicit class ZIOFuturesOps[A](zio : ZIO[Any , Throwable , A])  {
    def unsafeToFuture : Future[A] =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.runToFuture(zio)
      }
  }
}
