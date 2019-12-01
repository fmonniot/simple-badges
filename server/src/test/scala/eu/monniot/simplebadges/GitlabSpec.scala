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
import org.specs2.execute.Result
import org.specs2.matcher.MatchResult

class GitlabSpec extends Specification with CatsIO {
  def is = s2"""
  Gitlab[F] is a type-safe abstraction over the GitLab service API.
   
  #tags(Int): F[List[Gitlab.Tag]]
    can correctly parse the JSON returned by the API      $e1
    call the correct API                                  $e2
    include the configured oauth token when one is given  $e3
    wrap client errors in its own                         $e4

  #compare(Int, String, String): F[Gitlab.Comparison]
    call the correct API and parse the response JSON      $e5

  #commits(Int, String): F[List[Gitlab.Commit]]
    call the correct API and parse the response JSON      $e6
  """

  val jsonTagsResponseSample: String =
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

  val jsonCompareResponseSample: String =
    """{
      |  "commit": {
      |    "id": "12d65c8dd2b2676fa3ac47d955accc085a37a9c1",
      |    "short_id": "12d65c8dd2b",
      |    "title": "JS fix",
      |    "author_name": "Dmitriy Zaporozhets",
      |    "author_email": "dmitriy.zaporozhets@gmail.com",
      |    "created_at": "2014-02-27T10:27:00+02:00"
      |  },
      |  "commits": [{
      |    "id": "12d65c8dd2b2676fa3ac47d955accc085a37a9c1",
      |    "short_id": "12d65c8dd2b",
      |    "title": "JS fix",
      |    "author_name": "Dmitriy Zaporozhets",
      |    "author_email": "dmitriy.zaporozhets@gmail.com",
      |    "created_at": "2014-02-27T10:27:00+02:00"
      |  }],
      |  "diffs": [{
      |    "old_path": "files/js/application.js",
      |    "new_path": "files/js/application.js",
      |    "a_mode": null,
      |    "b_mode": "100644",
      |    "diff": "--- a/files/js/application.js\n+++ b/files/js/application.js\n@@ -24,8 +24,10 @@\n //= require g.raphael-min\n //= require g.bar-min\n //= require branch-graph\n-//= require highlightjs.min\n-//= require ace/ace\n //= require_tree .\n //= require d3\n //= require underscore\n+\n+function fix() { \n+  alert(\"Fixed\")\n+}",
      |    "new_file": false,
      |    "renamed_file": false,
      |    "deleted_file": false
      |  }],
      |  "compare_timeout": false,
      |  "compare_same_ref": false
      |}
      |""".stripMargin

  val jsonCommitsResponseSample: String =
    """[
      |  {
      |    "id": "ed899a2f4b50b4370feeea94676502b42383c746",
      |    "short_id": "ed899a2f4b5",
      |    "title": "Replace sanitize with escape once",
      |    "author_name": "Dmitriy Zaporozhets",
      |    "author_email": "dzaporozhets@sphereconsultinginc.com",
      |    "authored_date": "2012-09-20T11:50:22+03:00",
      |    "committer_name": "Administrator",
      |    "committer_email": "admin@example.com",
      |    "committed_date": "2012-09-20T11:50:22+03:00",
      |    "created_at": "2012-09-20T11:50:22+03:00",
      |    "message": "Replace sanitize with escape once",
      |    "parent_ids": [
      |      "6104942438c14ec7bd21c6cd5bd995272b3faff6"
      |    ]
      |  },
      |  {
      |    "id": "6104942438c14ec7bd21c6cd5bd995272b3faff6",
      |    "short_id": "6104942438c",
      |    "title": "Sanitize for network graph",
      |    "author_name": "randx",
      |    "author_email": "dmitriy.zaporozhets@gmail.com",
      |    "committer_name": "Dmitriy",
      |    "committer_email": "dmitriy.zaporozhets@gmail.com",
      |    "created_at": "2012-09-20T09:06:12+03:00",
      |    "message": "Sanitize for network graph",
      |    "parent_ids": [
      |      "ae1d9fb46aa2b07ee9836d49862ec4e2c46fbbba"
      |    ]
      |  }
      |]
      |""".stripMargin

  private val bearer = CaseInsensitiveString("bearer")

  val e1: IO[MatchResult[List[Gitlab.Tag]]] = {
    val gitlab = Gitlab.impl(
      Client.fromHttpApp(HttpApp[IO](_ => Ok(jsonTagsResponseSample))),
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
          Ok(jsonTagsResponseSample)
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
            Ok(jsonTagsResponseSample)
          case _ =>
            NotFound("Auth doesn't match")
        }
      }),
      Gitlab.GitlabConfig(uri"http://example.org", Some("my-token-api"))
    )

    gitlab.tags(42).map(_ must have length 1)
  }

  val e4: IO[Result] = {
    val gitlab = Gitlab.impl(
      Client.fromHttpApp(HttpApp[IO](_ => NotFound())),
      Gitlab.GitlabConfig(uri"http://example.org", None)
    )

    gitlab
      .tags(0)
      .attempt
      .map {
        case Right(_) => failure("unreachable")
        case Left(error) => error must haveClass[Gitlab.GitlabError]
      }
  }

  val e5: IO[MatchResult[Gitlab.Comparison]] = {
    val gitlab = Gitlab.impl(
      Client.fromHttpApp(HttpApp[IO] {
        case req @ GET -> Root / "projects" / "50" / "repository" / "compare" =>
          val result = for {
            from <- req.params.get("from") if from == "tag"
            to <- req.params.get("to") if to == "master"
          } yield (from, to)

          result.fold(NotFound("Incorrect query params"))(_ => Ok(jsonCompareResponseSample))
        case _ => NotFound()
      }),
      Gitlab.GitlabConfig(uri"http://example.org", None)
    )

    gitlab.compare(50, "tag", "master").map { comparison =>
      comparison must beEqualTo(
        Gitlab.Comparison(List(Gitlab.Commit("12d65c8dd2b2676fa3ac47d955accc085a37a9c1", "JS fix")))
      )
    }
  }

  val e6: IO[MatchResult[List[Gitlab.Commit]]] = {
    val gitlab = Gitlab.impl(
      Client.fromHttpApp(HttpApp[IO] {
        case req @ GET -> Root / "projects" / "50" / "repository" / "commits" =>
          req.params
            .get("ref_name")
            .filter(_ == "develop")
            .fold(NotFound("Incorrect query params"))(_ => Ok(jsonCommitsResponseSample))
        case _ => NotFound()
      }),
      Gitlab.GitlabConfig(uri"http://example.org", None)
    )

    gitlab.commits(50, "develop").map { comparison =>
      comparison must beEqualTo(
        List(
          Gitlab.Commit(
            "ed899a2f4b50b4370feeea94676502b42383c746",
            "Replace sanitize with escape once"
          ),
          Gitlab.Commit("6104942438c14ec7bd21c6cd5bd995272b3faff6", "Sanitize for network graph")
        )
      )
    }
  }
}
