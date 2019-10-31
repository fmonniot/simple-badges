package eu.monniot.simplebadges

import cats.effect.Sync
import eu.monniot.simplebadges.Config.HttpServerConfig
import eu.monniot.simplebadges.services.Gitlab.GitlabConfig
import org.http4s.Uri
import pureconfig.error.CannotConvert
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

case class Config(
    gitlab: GitlabConfig,
    http: HttpServerConfig
)

object Config {

  def load[F[_]: Sync]: F[Config] =
    ConfigSource.default.loadF[F, Config]

  case class HttpServerConfig(host: String, port: Int)

  // Imports the definition until `com.github.pureconfig:pureconfig-http4s`
  // get published for 2.13 (waiting for a stable http4s release)
  implicit val uriReader: ConfigReader[Uri] =
    ConfigReader.fromString(
      str =>
        Uri
          .fromString(str)
          .fold(
            err => Left(CannotConvert(str, "Uri", err.sanitized)),
            uri => Right(uri)))
}
