package eu.monniot.simplebadges.cache

import cats.effect.{Clock, Concurrent}
import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.implicits._
import cats.implicits._

import scala.concurrent.duration._

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
  case class Value[F[_], A](a: A, expireAt: Long) extends State[F, A]

  // Concurrent bound is too high (strictly: MonadError + Applicative),
  // but it's already required by the constructor so
  case class Updating[F[_]: Concurrent, A](update: Deferred[F, Either[Throwable, A]],
                                           previous: Option[A])
      extends State[F, A] {
    def value: F[A] = previous.fold(update.get.rethrow)(_.pure[F])
  }

  def create[F[_]: Concurrent, A](getA: F[A], expiration: FiniteDuration)(
      implicit clock: Clock[F]): F[Cached[F, A]] =
    Ref.of(NoValue[F, A](): State[F, A]).map { state =>
      new Cached[F, A] {
        override def get: F[A] =
          for {
            d <- Deferred[F, Either[Throwable, A]]
            time <- clock.monotonic(MILLISECONDS)
            a <- state.modify {
              case NoValue() =>
                // get new value and wait for it to be available
                Updating(d, None) -> fetch(d).rethrow

              case st @ Value(a, expireAt) =>
                // Refreshing values when the current one is expired in the background
                // and returning the current value immediately
                if (time > expireAt)
                  Updating(d, Some(a)) -> (fetch(d).attempt.start.void >> a.pure[F])
                else st -> a.pure[F]

              case st: Updating[F, A] =>
                // Value is being updated, return previous value or wait for new one to be available
                (st, st.value)
            }.flatten
          } yield a

        def fetch(
            d: Deferred[F, Either[Throwable, A]]
        ): F[Either[Throwable, A]] = {
          {
            for {
              res <- getA.attempt
              time <- clock.monotonic(MILLISECONDS)
              _ <- state.set {
                res match {
                  case Left(_) => NoValue()
                  case Right(a) =>
                    Value(a, time + expiration.toMillis)
                }
              }
              _ <- d.complete(res)
            } yield res
          }.onCancel {
            state.modify {
              case st @ Value(a, _) =>
                // we can't do another fetch on cancellation, so we are ignoring the expiration time
                st -> d.complete(Right(a)).attempt.void
              case NoValue() | Updating(_, _) =>
                val error = new RuntimeException("Fetching cancelled, couldn't retrieve value")
                NoValue[F, A]() -> d.complete(Left(error)).attempt.void
            }
          }

        }
      }
    }
}
