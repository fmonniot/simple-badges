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
import cats.effect.concurrent.Ref
import cats.implicits._

import scala.concurrent.duration.FiniteDuration

sealed trait CacheMap[F[_], K, V] {
  def get(key: K): F[V]
}

object CacheMap {

  def create[F[_]: Concurrent: Clock, K, V](keyToValue: K => F[V],
                                            expiration: FiniteDuration): F[CacheMap[F, K, V]] =
    Ref.of(Map.empty[K, Cached[F, V]]).map { state =>
      new CacheMap[F, K, V] {

        def cached(key: K): F[Cached[F, V]] =
          state.get.map { map =>
            map.get(key) match {
              case Some(cached) =>
                cached.pure[F] // There is already a cached value
              case None =>
                // There is no cached value, creating a new one
                Cached.create(keyToValue(key), expiration).flatMap { cached =>
                  state.modify { m =>
                    // We verify a second time no concurrent update have been made
                    m.get(key) match {
                      case Some(a) => m -> a
                      case None => m + (key -> cached) -> cached
                    }
                  }
                }
            }
          }.flatten

        override def get(key: K): F[V] =
          cached(key).flatMap(_.get)
      }

    }
}
