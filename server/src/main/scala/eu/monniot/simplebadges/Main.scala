package eu.monniot.simplebadges

import cats.effect._
import cats.implicits._
import eu.monniot.badges.WidthTable
import eu.monniot.simplebadges.http.{BadgesRoutes, SimplebadgesRoutes}
import eu.monniot.simplebadges.services.{Gitlab, HelloWorld, Jokes}
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    server[IO].compile.lastOrError

  def server[F[_]: ConcurrentEffect](implicit T: Timer[F],
                                     C: ContextShift[F]): Stream[F, ExitCode] =
    for {
      client <- BlazeClientBuilder[F](global).stream
      helloWorldAlg = HelloWorld.impl[F]
      jokeAlg = Jokes.impl[F](client)
      gitlab = Gitlab.impl(
        client,
        Gitlab.GitlabConfig(uri"https://gitlab.com/", ""))

      blocker <- Stream.resource(Blocker[F])
      widthTable <- Stream.eval(WidthTable.verdanaTable[F](blocker))

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = Router(
        "/api" -> (SimplebadgesRoutes.helloWorldRoutes(helloWorldAlg) <+>
          SimplebadgesRoutes.jokeRoutes(jokeAlg)),
        "/badges" -> (BadgesRoutes.generic[F](widthTable) <+> BadgesRoutes
          .gitlab(widthTable, gitlab))
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
}
