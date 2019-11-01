package eu.monniot.simplebadges.services

import cats.effect.Concurrent
import cats.implicits._
import eu.monniot.simplebadges.cache.CacheMap
import eu.monniot.simplebadges.services.Gitlab.Tag

sealed trait TagCache[F[_]] {
  def latest(projectId: Int): F[Option[Tag]]
}

object TagCache {

  def create[F[_]: Concurrent](gitlab: Gitlab[F]): F[TagCache[F]] =
    CacheMap
      .create((id: Int) => gitlab.tags(id).map(_.headOption))
      .map { cache =>
        new TagCache[F] {
          override def latest(projectId: Int): F[Option[Tag]] =
            cache.get(projectId)
        }
      }

}
