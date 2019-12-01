package eu.monniot.simplebadges.http

import cats.implicits._
import cats.effect.{Blocker, IO}
import cats.effect.specs2.CatsIO
import eu.monniot.badges.WidthTable
import eu.monniot.simplebadges.services.Gitlab.Tag
import eu.monniot.simplebadges.services.{Gitlab, TagCache}
import org.http4s._
import org.http4s.implicits._
import org.http4s.headers.`Content-Type`
import org.specs2.Specification
import org.specs2.execute.Result
import org.specs2.matcher.MatchResult

class BadgesRoutesSpec extends Specification with CatsIO {

  def is = s2"""
  BadgeRoutes defines some routes related to the badge library
  
  `generic` offer APIs to render generic badges
    with only a message          $genericBadgesWithMessage
    with a message and a label   $genericBadgesWithMessageAndLabel
    
  `gitlab` offer APIs to render badges based on GitLab status
    version:
      using a project's latest tag as its version  $gitlabTagBadgeWithTag
      render a default badge when no tag was found $gitlabTagBadgeWithNoTag
    unreleased commits:
      render the number of commits when no tag found     $gitlabUnreleasedNoTag
      display the number of commits between head and ref $gitlabUnreleasedCommitsNumber
      change the color based on the number of commits    $gitlabUnreleasedCommitsColors
  """

  val table: IO[WidthTable] = Blocker[IO].use(blocker => WidthTable.verdanaTable[IO](blocker))

  // Generic APIs

  val genericBadgesWithMessage: IO[MatchResult[Any]] = {
    table
      .map(BadgesRoutes.generic[IO](_).orNotFound)
      .flatMap { routes =>
        routes(Request(Method.GET, uri"/generic/my%20message"))
      }
      .map { response =>
        val st = response.status must beEqualTo(Status.Ok)
        val ct = response.contentType must beSome(`Content-Type`(MediaType.application.xml))
        val bo = response.as[String].unsafeRunSync() must contain("my message")

        st and ct and bo
      }
  }

  val genericBadgesWithMessageAndLabel: IO[MatchResult[Any]] = {
    table
      .map(BadgesRoutes.generic[IO](_).orNotFound)
      .flatMap { routes =>
        routes(Request(Method.GET, uri"/generic/label/my%20message"))
      }
      .map { response =>
        val st = response.status must beEqualTo(Status.Ok)
        val ct = response.contentType must beSome(`Content-Type`(MediaType.application.xml))
        val bo = response.as[String].unsafeRunSync() must contain("my message") and contain("label")

        st and ct and bo
      }
  }

  // Gitlab APIs

  val fixedTagCache: TagCache[IO] = {
    case i if i < 10 => IO(None)
    case 42 => IO(Some(Tag("v1.3.2", "17ffe0e699ea66c894cd4d6abe231df5f86dc4ca")))
    case _ => IO(Some(Tag("my-tag", "bbb")))
  }

  val gitlab: Gitlab[IO] = new Gitlab[IO] {
    override def tags(projectId: Int): IO[List[Tag]] = IO.raiseError(failure.exception)

    override def compare(projectId: Int, from: String, to: String): IO[Gitlab.Comparison] =
      (projectId, from, to) match {
        case (i, _, "numbers") =>
          IO(Gitlab.Comparison((1 to i).map(_ => Gitlab.Commit("aaa", "Update CI")).toList))
        case (i, _, "colors") =>
          IO(Gitlab.Comparison((1 to (i - 100)).map(_ => Gitlab.Commit("aaa", "Update CI")).toList))
        case _ => IO.raiseError(failure.exception)
      }

    override def commits(projectId: Int, ref: String): IO[List[Gitlab.Commit]] =
      (projectId, ref) match {
        case (i, _) =>
          IO((1 to i).map(_ => Gitlab.Commit("aaa", "Update CI")).toList)

        case _ => IO.raiseError(failure.exception)
      }
  }

  val gitlabTagBadgeWithTag: IO[MatchResult[Any]] =
    table
      .map(BadgesRoutes.gitlab[IO](_, fixedTagCache, gitlab).orNotFound)
      .flatMap(routes => routes(Request(Method.GET, uri"/gitlab/42/tag")))
      .map { response =>
        val st = response.status must beEqualTo(Status.Ok)
        val ct = response.contentType must beSome(`Content-Type`(MediaType.application.xml))
        val bo = response.as[String].unsafeRunSync() must contain("version") and contain("1.3.2")

        st and ct and bo
      }

  val gitlabTagBadgeWithNoTag: IO[MatchResult[Any]] =
    table
      .map(BadgesRoutes.gitlab[IO](_, fixedTagCache, gitlab).orNotFound)
      .flatMap(routes => routes(Request(Method.GET, uri"/gitlab/0/tag")))
      .map { response =>
        val st = response.status must beEqualTo(Status.Ok)
        val ct = response.contentType must beSome(`Content-Type`(MediaType.application.xml))
        val str = response.as[String].unsafeRunSync()
        val bo1 = str must not contain "version" and not contain "1.3.2"
        val bo2 = str must contain("No tags found") and contain("lightgrey")

        st and ct and bo1 and bo2
      }

  val gitlabUnreleasedNoTag: IO[MatchResult[Any]] =
    table
      .map(BadgesRoutes.gitlab[IO](_, fixedTagCache, gitlab).orNotFound)
      .flatMap(routes => routes(Request(Method.GET, uri"/gitlab/9/unreleased/master")))
      .map { response =>
        val st = response.status must beEqualTo(Status.Ok)
        val ct = response.contentType must beSome(`Content-Type`(MediaType.application.xml))
        val bo = response.as[String].unsafeRunSync() must contain("9 unreleased") and contain(
          "lightgrey"
        )

        st and ct and bo
      }

  val gitlabUnreleasedCommitsNumber: IO[MatchResult[Any]] =
    table
      .map(BadgesRoutes.gitlab[IO](_, fixedTagCache, gitlab).orNotFound)
      .flatMap(routes => routes(Request(Method.GET, uri"/gitlab/747/unreleased/numbers")))
      .map { response =>
        val st = response.status must beEqualTo(Status.Ok)
        val ct = response.contentType must beSome(`Content-Type`(MediaType.application.xml))
        val bo = response
          .as[String]
          .unsafeRunSync() must contain("747 unreleased") and not contain "lightgrey"

        st and ct and bo
      }

  val gitlabUnreleasedCommitsColors: IO[Result] = {
    def testOne(n: Int, expectedColor: String) =
      table
        .map(BadgesRoutes.gitlab[IO](_, fixedTagCache, gitlab).orNotFound)
        .flatMap(
          routes =>
            routes(Request(Method.GET, Uri.unsafeFromString(s"/gitlab/$n/unreleased/colors")))
        )
        .map { response =>
          val st = response.status must beEqualTo(Status.Ok)
          val ct = response.contentType must beSome(`Content-Type`(MediaType.application.xml))
          val bo = response.as[String].unsafeRunSync() must contain(expectedColor)

          (st and ct and bo).toResult
        }

    Map(
      100 + 0 -> "green",
      100 + 2 -> "green",
      100 + 3 -> "orange",
      100 + 9 -> "orange",
      100 + 10 -> "red"
    ).map(testOne _ tupled)
      .toList
      .sequence
      .map(_.fold(success)(_ and _))
  }

}
