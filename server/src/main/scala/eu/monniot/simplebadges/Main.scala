package eu.monniot.simplebadges

import cats.effect._
import cats.implicits._
import eu.monniot.badges.WidthTable
import eu.monniot.simplebadges.http.BadgesRoutes
import eu.monniot.simplebadges.services.{Gitlab, TagCache}
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{Logger => ClientLogger}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{Logger => ServerLogger}

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    server[IO].compile.lastOrError

  def server[F[_]: ConcurrentEffect](implicit T: Timer[F],
                                     C: ContextShift[F]): Stream[F, ExitCode] =
    for {
      config <- Stream.eval(Config.load[F])
      client <- BlazeClientBuilder[F](global).stream
        .map(ClientLogger(logHeaders = true, logBody = true))
      blocker <- Stream.resource(Blocker[F])

      gitlab = Gitlab.impl(client, config.gitlab)

      widthTable <- Stream.eval(WidthTable.verdanaTable[F](blocker))

      tagCache <- Stream.eval(TagCache.create(gitlab, config.tagCache))

      httpApp = Router(
        "/badges" -> {
          BadgesRoutes.generic[F](widthTable) <+>
            BadgesRoutes.gitlab(widthTable, tagCache, gitlab)
        }
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = ServerLogger.httpApp(logHeaders = true, logBody = false)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
}
