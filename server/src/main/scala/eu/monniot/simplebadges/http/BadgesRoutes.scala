package eu.monniot.simplebadges.http

import cats.effect.Sync
import cats.implicits._
import eu.monniot.badges.WidthTable
import eu.monniot.badges.rendering.badges
import eu.monniot.simplebadges.services.TagCache
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

  def gitlab[F[_]: Sync](table: WidthTable, tagCache: TagCache[F]): HttpRoutes[F] = {
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
        val gitlab: eu.monniot.simplebadges.services.Gitlab[F] = ???

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
              gitlab
                .compare(projectId, ref, tag.name)
                .map(_.commits.size)
                .map {
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
