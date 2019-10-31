package eu.monniot.badges

import java.nio.file.Paths

import cats.effect.{Blocker, IO}
import org.specs2.Specification
import org.specs2.specification.core.SpecStructure

class WidthTableSpec extends Specification {
  override def is: SpecStructure =
    s2"""
      A WidthTable can return the width of
        a known character $e1 (not in a range)
        a known character $e2 (in a range)
        a known string $e3

      A WidthTable declare a private custom ordering which
        return 0 when the x is in the y range $a1
      """

  import WidthTableSpec.unsafeVerdanaTable

  def e1 = unsafeVerdanaTable.widthOfCharCode('m') must beSome(106.99)
  def e2 = unsafeVerdanaTable.widthOfCharCode('1') must beSome(69.93)

  def e3 =
    WidthTable.widthOf(unsafeVerdanaTable, "v1.2.511") must beRight(
      between(494.77 - 0.1, 494.77 + 0.1))

  def a1 =
    WidthTable.ordering.compare((49, 49, 0.0), (48, 57, 1.0)) must beEqualTo(0)
}

object WidthTableSpec {

  def unsafeVerdanaTable: WidthTable = {
    implicit val cs = IO.contextShift(scala.concurrent.ExecutionContext.global)
    val blocker = Blocker[IO]

    val te = blocker.use(b => WidthTable.verdanaTable[IO](b))

    te.unsafeRunSync()
  }
}
