package eu.monniot.simplebadges.cache

import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.effect.specs2.CatsIO
import org.specs2.Specification
import org.specs2.matcher.MatchResult

import scala.concurrent.duration._

class CachedSpec extends Specification with CatsIO {
  def is = s2"""
  Cached is a data structure to cache a F[A] computation.
  It can be think of a finite state machine between three states:
  no value, updating value and got a value.
  This specification describe some state transition
    get a value for the first time              $e1
    get a value before the expiration happened  $e2
    get a value after the expiration happened   $e3

  """

  val e1: IO[MatchResult[Int]] =
    for {
      ref <- Ref.of[IO, Int](0)
      fa = ref.modify(i => (i + 1, i))
      cached <- Cached.create(fa, 1.minute)

      value <- cached.get
    } yield value must beEqualTo(0)

  val e2: IO[MatchResult[Int]] =
    for {
      ref <- Ref.of[IO, Int](0)
      fa = ref.modify(i => (i + 1, i))
      cached <- Cached.create(fa, 1.minute)

      value <- cached.get
      value2 <- cached.get
    } yield (value must beEqualTo(0)) and (value2 must beEqualTo(0))

  val e3: IO[MatchResult[Int]] =
    for {
      ref <- Ref.of[IO, Int](0)
      fa = ref.modify(i => (i + 1, i))
      cached <- Cached.create(fa, 1.millisecond)

      // First get we return the computed value
      r1 <- cached.get.map(_ must beEqualTo(0))
      // Then we wait a bit for the value to be expired
      _ <- IO.sleep(2.milliseconds)
      // Second check after expiration, we return the stale value and fetch in background
      r2 <- cached.get.map(_ must beEqualTo(0))
      // Third check, we now have the new value in cache
      r3 <- cached.get.map(_ must beEqualTo(1))
    } yield r1 and r2 and r3
}
