package model

import java.util.UUID

trait Generator[A] {
  def next(): A
}

object Generator {
  def apply[A](implicit
      gen: Generator[A]): Generator[A] = implicitly

  def from[F, T](f: F => T)(implicit
      gen: Generator[F]): Generator[T] =
    () => f(gen.next)

  implicit val uuidGen: Generator[UUID] = () => UUID.randomUUID()
}
