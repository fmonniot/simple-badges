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
