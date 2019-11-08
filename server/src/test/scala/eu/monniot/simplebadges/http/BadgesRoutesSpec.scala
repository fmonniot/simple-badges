package eu.monniot.simplebadges.http

import cats.effect.{Blocker, IO}
import cats.effect.specs2.CatsIO
import eu.monniot.badges.WidthTable
import eu.monniot.simplebadges.services.Gitlab.Tag
import eu.monniot.simplebadges.services.TagCache
import org.http4s._
import org.http4s.implicits._
import org.http4s.headers.`Content-Type`
import org.specs2.Specification
import org.specs2.matcher.MatchResult

class BadgesRoutesSpec extends Specification with CatsIO {

  def is = s2"""
  BadgeRoutes defines some routes related to the badge library
  
  `generic` offer APIs to render generic badges
    with only a message          $genericBadgesWithMessage
    with a message and a label   $genericBadgesWithMessageAndLabel
    
  `gitlab` offer APIs to render badges based on GitLab status
    using a project's latest tag as its version  $gitlabBadgeWithTag
    render a default badge when no tag was found $gitlabBadgeWithNoTag
  """

  val table: IO[WidthTable] = Blocker[IO].use(blocker => WidthTable.verdanaTable[IO](blocker))

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

  val fixedTagCache: TagCache[IO] = {
    case 42 => IO(Some(Tag("v1.3.2", "17ffe0e699ea66c894cd4d6abe231df5f86dc4ca")))
    case _ => IO(None)
  }

  val gitlabBadgeWithTag: IO[MatchResult[Any]] =
    table
      .map(BadgesRoutes.gitlab[IO](_, fixedTagCache).orNotFound)
      .flatMap(routes => routes(Request(Method.GET, uri"/gitlab/42/tag")))
      .map { response =>
        val st = response.status must beEqualTo(Status.Ok)
        val ct = response.contentType must beSome(`Content-Type`(MediaType.application.xml))
        val bo = response.as[String].unsafeRunSync() must contain("version") and contain("1.3.2")

        st and ct and bo
      }

  val gitlabBadgeWithNoTag: IO[MatchResult[Any]] =
    table
      .map(BadgesRoutes.gitlab[IO](_, fixedTagCache).orNotFound)
      .flatMap(routes => routes(Request(Method.GET, uri"/gitlab/0/tag")))
      .map { response =>
        val st = response.status must beEqualTo(Status.Ok)
        val ct = response.contentType must beSome(`Content-Type`(MediaType.application.xml))
        val str = response.as[String].unsafeRunSync()
        val bo1 = str must not contain "version" and not contain "1.3.2"
        val bo2 = str must contain("No tags found") and contain("lightgrey")

        st and ct and bo1 and bo2
      }

}
