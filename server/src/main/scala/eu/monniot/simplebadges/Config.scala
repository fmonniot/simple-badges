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

package eu.monniot.simplebadges

import cats.effect.Sync
import eu.monniot.simplebadges.Config.HttpServerConfig
import eu.monniot.simplebadges.services.Gitlab.GitlabConfig
import eu.monniot.simplebadges.services.TagCache.TagCacheConfig
import org.http4s.Uri
import pureconfig.error.CannotConvert
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

case class Config(
    gitlab: GitlabConfig,
    http: HttpServerConfig,
    tagCache: TagCacheConfig
)

object Config {

  def load[F[_]: Sync]: F[Config] =
    ConfigSource.default.loadF[F, Config]

  case class HttpServerConfig(host: String, port: Int)

  // Imports the definition until `com.github.pureconfig:pureconfig-http4s`
  // get published for 2.13 (waiting for a stable http4s release)
  implicit val uriReader: ConfigReader[Uri] =
    ConfigReader.fromString { str =>
      Uri
        .fromString(str)
        .fold(err => Left(CannotConvert(str, "Uri", err.sanitized)), uri => Right(uri))
    }
}
