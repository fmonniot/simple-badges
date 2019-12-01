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

package eu.monniot.simplebadges.http

import cats.effect.Sync
import cats.implicits._
import eu.monniot.badges.WidthTable
import eu.monniot.badges.rendering.badges
import eu.monniot.simplebadges.services.{Gitlab, TagCache}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object BadgesRoutes {

  import org.http4s.scalaxml._

  def generic[F[_]: Sync](table: WidthTable): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "generic" / label / message =>
        Ok(badges.flat(table, message, label.some))

      case GET -> Root / "generic" / message =>
        Ok(badges.flat(table, message))
    }
  }

  def gitlab[F[_]: Sync](table: WidthTable,
                         tagCache: TagCache[F],
                         gitlab: Gitlab[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    import eu.monniot.badges.rendering.Color._

    HttpRoutes.of[F] {
      case GET -> Root / "gitlab" / IntVar(projectId) / "tag" =>
        tagCache
          .latest(projectId)
          .map {
            case Some(tag) =>
              badges.flat(table, message = tag.name.stripPrefix("v"), label = Some("version"))
            case None =>
              badges.flat(table, message = "No tags found", messageColor = Some(color"lightgrey"))
          }
          .flatMap(Ok(_))

      case GET -> Root / "gitlab" / IntVar(projectId) / "unreleased" / ref =>
        tagCache
          .latest(projectId)
          .flatMap {
            case None =>
              // Lookup `ref` and count the number of commits on it
              gitlab
                .commits(projectId, ref)
                .map(_.size)
                .map(count => (s"$count unreleased", color"lightgrey"))

            case Some(tag) =>
              // We assume the tag must be in the ref history, hence
              // going from it to the start of the reference.
              gitlab
                .compare(projectId, from = tag.name, to = ref)
                .map(_.commits.size)
                .map {
                  // Here it should probably be a configuration options instead
                  case 0 =>
                    ("caught up", color"green")
                  case i if i < 3 =>
                    (s"$i unreleased", color"green")
                  case i if i < 10 =>
                    (s"$i unreleased", color"orange")
                  case i =>
                    (s"$i unreleased", color"red")
                }
          }
          .map {
            case (message, color) =>
              badges.flat(
                table,
                label = Some("commits"),
                message = message,
                messageColor = Some(color)
              )
          }
          .flatMap(Ok(_))
    }
  }
}
