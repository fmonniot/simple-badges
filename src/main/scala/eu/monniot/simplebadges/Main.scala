package eu.monniot.simplebadges

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}
import cats.implicits._
import eu.monniot.simplebadges.http.SimplebadgesRoutes
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
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

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        SimplebadgesRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
          SimplebadgesRoutes.jokeRoutes[F](jokeAlg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
}
