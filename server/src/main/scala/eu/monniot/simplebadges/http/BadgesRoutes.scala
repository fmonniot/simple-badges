package eu.monniot.simplebadges.http

import cats.effect.Sync
import cats.implicits._
import eu.monniot.badges.WidthTable
import eu.monniot.badges.rendering.badges
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object BadgesRoutes {

  import org.http4s.scalaxml._

  def custom[F[_]: Sync](table: WidthTable): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "badge" / label / message =>
        Ok(badges.flat(table, message, label.some))

      case GET -> Root / "badge" / message =>
        Ok(badges.flat(table, message))
    }
  }
}
