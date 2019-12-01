/*
 * Copyright 2019 François Monniot
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            }.flatten
          }

        }
      }
    }
}
