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

import cats.implicits._
import cats.effect.Sync
import io.circe.generic.semiauto._
import io.circe.Decoder
import org.http4s.Credentials.Token
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.http4s.util.CaseInsensitiveString
import org.http4s.{EntityDecoder, Headers, Request, Uri}

import scala.util.control.NoStackTrace

trait Gitlab[F[_]] {
  def tags(projectId: Int): F[List[Gitlab.Tag]]

  // from & to are git references
  def compare(projectId: Int, from: String, to: String): F[Gitlab.Comparison]

  def commits(projectId: Int, ref: String): F[List[Gitlab.Commit]]
}

object Gitlab {
  def apply[F[_]](implicit ev: Gitlab[F]): Gitlab[F] = ev

  final case class Tag(name: String, target: String)
  object Tag {
    implicit val tagDecoder: Decoder[Tag] = deriveDecoder
  }

  final case class Commit(id: String, title: String)
  object Commit {
    implicit val commitDecoder: Decoder[Commit] = deriveDecoder
  }

  final case class Comparison(commits: List[Commit])
  object Comparison {
    implicit val comparisonDecoder: Decoder[Comparison] = deriveDecoder
  }

  final case class GitlabError(e: Throwable) extends RuntimeException(e) with NoStackTrace

  case class GitlabConfig(base: Uri, token: Option[String])

  def impl[F[_]: Sync](client: Client[F], config: GitlabConfig): Gitlab[F] =
    new Gitlab[F] {

      // This API only works with JSON, so let's bring in a generic entity decoder
      implicit private def jsonED[T: Decoder]: EntityDecoder[F, T] =
        jsonOf[F, T]

      private def authorization =
        config.token.fold(Headers.empty) { value =>
          Headers.of(
            Authorization(Token(CaseInsensitiveString("Bearer"), value))
          )
        }

      override def tags(projectId: Int): F[List[Tag]] =
        client
          .expect[List[Tag]](
            Request[F](
              method = GET,
              uri = config.base / "projects" / projectId.toString / "repository" / "tags",
              headers = authorization
            )
          )
          .adaptError { case t => GitlabError(t) }

      override def compare(projectId: Int, from: String, to: String): F[Comparison] =
        client
          .expect[Comparison](
            Request[F](
              method = GET,
              uri = config.base / "projects" / projectId.toString / "repository" / "compare" +? ("from", from) +? ("to", to),
              headers = authorization
            )
          )
          .adaptError { case t => GitlabError(t) }

      override def commits(projectId: Int, ref: String): F[List[Commit]] =
        client
          .expect[List[Commit]](
            Request[F](
              method = GET,
              uri = config.base / "projects" / projectId.toString / "repository" / "commits" +? ("ref_name", ref),
              headers = authorization
            )
          )
          .adaptError { case t => GitlabError(t) }
    }
}
