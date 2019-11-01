package eu.monniot.simplebadges.cache

import cats.effect.Concurrent
import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.implicits._
import cats.implicits._

trait Cached[F[_], A] {
  def get: F[A]
}

/**
 * Largely inspired from Fabio Labella's talk at Scala Italy 2019.
 * Original code can be found on GitHub:
 * https://github.com/SystemFw/scala-italy-2019/blob/db0a68f5b8443516b8c5f4b2a5156eb107e3cba5/Examples.scala
 */
object Cached {

  sealed trait State[F[_], A]
  case class NoValue[F[_], A]() extends State[F, A]
  case class Value[F[_], A](a: A) extends State[F, A]
  case class Updating[F[_], A](update: Deferred[F, Either[Throwable, A]])
      extends State[F, A]

  def create[F[_]: Concurrent, A](getA: F[A]): F[Cached[F, A]] = {

    Ref.of(NoValue[F, A](): State[F, A]).map { state =>
      new Cached[F, A] {
        override def get: F[A] =
          Deferred[F, Either[Throwable, A]].flatMap { d =>
            state.modify {
              case NoValue() =>
                // TODO Here we will probably want to make a distinction between initial fetch
                // TODO and a refresh of the value (no previous value vs one)
                Updating(d) -> fetch(d).rethrow

              case s @ Value(a) =>
                s -> a.pure[F] // TODO Also needs to start an update to refresh

              case s @ Updating(d) =>
                (s, d.get.rethrow)
            }.flatten
          }

        def fetch(
            d: Deferred[F, Either[Throwable, A]]
        ): F[Either[Throwable, A]] = {
          {
            for {
              res <- getA.attempt
              _ <- state.set {
                res match {
                  case Left(_) => NoValue()
                  case Right(a) => Value(a)
                }
              }
              _ <- d.complete(res)
            } yield res
          }.onCancel {
            state.modify {
              case st @ Value(a) =>
                st -> d.complete(Right(a)).attempt.void
              case NoValue() | Updating(_) =>
                val error = new RuntimeException(
                  "Fetching cancelled, couldn't retrieve value")
                NoValue[F, A]() -> d.complete(Left(error)).attempt.void
            }
          }

        }
      }
    }
  }
}
