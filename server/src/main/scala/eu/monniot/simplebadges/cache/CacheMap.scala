package eu.monniot.simplebadges.cache

import cats.effect.Concurrent
import cats.effect.concurrent.Ref
import cats.implicits._

sealed trait CacheMap[F[_], K, V] {
  def get(key: K): F[V]
}

object CacheMap {

  def create[F[_]: Concurrent, K, V](fa: K => F[V]): F[CacheMap[F, K, V]] =
    Ref.of(Map.empty[K, Cached[F, V]]).map { state =>
      new CacheMap[F, K, V] {

        def cached(key: K): F[Cached[F, V]] =
          state.get.map { map =>
            map.get(key) match {
              case Some(cached) =>
                cached.pure[F] // There is already a cached value
              case None =>
                // There is no cached value, creating a new one
                Cached.create(fa(key)).flatMap { cached =>
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
