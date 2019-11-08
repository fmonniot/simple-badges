package eu.monniot.simplebadges

import cats.effect.IO
import cats.effect.specs2.CatsIO
import eu.monniot.simplebadges.services.Gitlab
import org.http4s.{Credentials, HttpApp}
import org.http4s.client.Client
import org.specs2.Specification
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.http4s.util.CaseInsensitiveString
import org.specs2.matcher.MatchResult

class GitlabSpec extends Specification with CatsIO {
  def is = s2"""
  Gitlab[F] is a type-safe abstraction over the GitLab service API.
   
  #tags(Int): F[List[Gitlab.Tag]]
    can correctly parse the JSON returned by the API      $e1
    call the correct API                                  $e2
    include the configured oauth token when one is given  $e3
  """

  val jsonResponseSample: String =
    """[
      |  {
      |    "commit": {
      |      "id": "2695effb5807a22ff3d138d593fd856244e155e7",
      |      "short_id": "2695effb",
      |      "title": "Initial commit",
      |      "created_at": "2017-07-26T11:08:53.000+02:00",
      |      "parent_ids": [
      |        "2a4b78934375d7f53875269ffd4f45fd83a84ebe"
      |      ],
      |      "message": "Initial commit",
      |      "author_name": "John Smith",
      |      "author_email": "john@example.com",
      |      "authored_date": "2012-05-28T04:42:42-07:00",
      |      "committer_name": "Jack Smith",
      |      "committer_email": "jack@example.com",
      |      "committed_date": "2012-05-28T04:42:42-07:00"
      |    },
      |    "release": {
      |      "tag_name": "1.0.0",
      |      "description": "Amazing release. Wow"
      |    },
      |    "name": "v1.0.0",
      |    "target": "2695effb5807a22ff3d138d593fd856244e155e7",
      |    "message": null,
      |    "protected": true
      |  }
      |]""".stripMargin

  val bearer = CaseInsensitiveString("bearer")

  val e1: IO[MatchResult[List[Gitlab.Tag]]] = {
    val gitlab = Gitlab.impl(
      Client.fromHttpApp(HttpApp[IO](_ => Ok(jsonResponseSample))),
      Gitlab.GitlabConfig(uri"http://example.org", None)
    )

    gitlab.tags(0).map { tags =>
      tags must beEqualTo(List(Gitlab.Tag("v1.0.0", "2695effb5807a22ff3d138d593fd856244e155e7")))
    }
  }

  val e2: IO[MatchResult[List[Gitlab.Tag]]] = {
    val gitlab = Gitlab.impl(
      Client.fromHttpApp(HttpApp[IO] {
        case GET -> Root / "projects" / "42" / "repository" / "tags" =>
          Ok(jsonResponseSample)
        case _ => NotFound()
      }),
      Gitlab.GitlabConfig(uri"http://example.org", None)
    )

    gitlab.tags(42).map(_ must have length 1)
  }

  val e3: IO[MatchResult[List[Gitlab.Tag]]] = {
    val gitlab = Gitlab.impl(
      Client.fromHttpApp(HttpApp[IO] { req =>
        req.headers.get(Authorization) match {
          case Some(Authorization(Credentials.Token(`bearer`, "my-token-api"))) =>
            Ok(jsonResponseSample)
          case _ =>
            NotFound("Auth doesn't match")
        }
      }),
      Gitlab.GitlabConfig(uri"http://example.org", Some("my-token-api"))
    )

    gitlab.tags(42).map(_ must have length 1)
  }

  val e4: IO[MatchResult[List[Gitlab.Tag]]] = {
    val gitlab = Gitlab.impl(
      Client.fromHttpApp(HttpApp[IO](_ => NotFound())),
      Gitlab.GitlabConfig(uri"http://example.org", None)
    )

    gitlab
      .tags(0)
      .map(_ => failure("unreachable"))
      .recov
  }

}
