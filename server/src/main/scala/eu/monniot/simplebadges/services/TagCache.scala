package eu.monniot.simplebadges.services

import cats.effect.{Clock, Concurrent}
import cats.implicits._
import eu.monniot.simplebadges.cache.CacheMap
import eu.monniot.simplebadges.services.Gitlab.Tag

import scala.concurrent.duration.FiniteDuration

trait TagCache[F[_]] {
  def latest(projectId: Int): F[Option[Tag]]
}

object TagCache {

  case class TagCacheConfig(keyExpiration: FiniteDuration)

  def create[F[_]: Concurrent: Clock](gitlab: Gitlab[F], config: TagCacheConfig): F[TagCache[F]] =
    CacheMap
      .create((id: Int) => gitlab.tags(id).map(_.headOption), config.keyExpiration)
      .map(cache => (projectId: Int) => cache.get(projectId))

}
