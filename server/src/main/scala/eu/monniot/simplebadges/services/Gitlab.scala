package eu.monniot.simplebadges.services

import cats.implicits._
import cats.effect.Sync
import io.circe.generic.semiauto._
import io.circe.Decoder
import org.http4s.Credentials.Token
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{EntityDecoder, EntityEncoder, Headers, Request, Uri}

trait Gitlab[F[_]] {
  def tags(projectId: Int): F[List[Gitlab.Tag]]
}

object Gitlab {
  def apply[F[_]](implicit ev: Gitlab[F]): Gitlab[F] = ev

  final case class Tag(name: String, target: String)

  object Tag {
    implicit val tagDecoder: Decoder[Tag] = deriveDecoder
  }

  final case class GitlabError(e: Throwable) extends RuntimeException

  case class GitlabConfig(base: Uri, token: String)

  def impl[F[_]: Sync](client: Client[F], config: GitlabConfig): Gitlab[F] =
    new Gitlab[F] {
      private val dsl = new Http4sClientDsl[F] {}
      import dsl._

      // This API only works with JSON, so let's bring in a generic entity decoder
      implicit private def jsonED[T: Decoder]: EntityDecoder[F, T] =
        jsonOf[F, T]

      override def tags(projectId: Int): F[List[Tag]] =
        client
          .expect[List[Tag]](
            Request[F](
              method = GET,
              uri = config.base / "projects" / projectId.toString / "repository" / "tags",
              headers = Headers.of(Authorization(
                Token(CaseInsensitiveString("Bearer"), config.token)))
            ))
          .adaptError { case t => GitlabError(t) }
    }
}
