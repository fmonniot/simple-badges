package eu.monniot.simplebadges.characters

import java.nio.file.Paths

import cats.data.NonEmptyList
import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import io.circe
import io.circe.{DecodingFailure, Json}

import scala.collection.Searching

case class WidthTable private (data: Vector[(Long, Long, Double)]) {

  private[characters] lazy val emWidth: Double =
    widthOfCharCode('m').get

  def widthOfCharCode(char: Char): Option[Double] = {
    if (char.isControl) return Some(0.0)

    val codePoint = char.toLong
    data.search((codePoint, codePoint, 0.0))(WidthTable.ordering) match {
      case Searching.Found(foundIndex) =>
        data.get(foundIndex.toLong).map { case (_, _, width) => width }
      case _ =>
        None
    }
  }
}

object WidthTable {

  private[characters] val ordering = new Ordering[(Long, Long, Double)] {
    /*

      A special kind of ordering just for our use case.
      We assume that the Vector does not contains duplicate (ie. cannot have 1,2 and 1,3 as member).
      Because it is used in this controlled environment, we know that x is actually the element we are looking
      for. So we only use x._1 to see if it is included in the range of y._1 => y._2
      We return 0 whenever the x is contained in the y.

      Truth Table
      | x   | y   | result |
      | 1/1 | 2/3 | -1
      | 2/2 | 2/3 | 0
      | 3/3 | 2/4 | 0
      | 4/4 | 2/3 | 1
     */
    override def compare(x: (Long, Long, Double),
                         y: (Long, Long, Double)): Int = {
      if (x._1 < y._1) -1
      else if (x._1 > y._2) 1
      else 0
    }
  }

  def verdanaTable[F[_]: ContextShift](blocker: Blocker)(
      implicit F: Sync[F]): F[WidthTable] =
    for {
      path <- F.delay(Paths.get {
        WidthTable.getClass.getClassLoader
          .getResource("verdana-table.json")
          .toURI
      })
      table <- WidthTable
        .fromStream(fs2.io.file.readAll(path, blocker, chunkSize = 1024))
        .flatMap {
          case Left(e) => F.raiseError[WidthTable](e)
          case Right(t) => F.pure(t)
        }
    } yield table

  def fromStream[F[_]: Sync](
      stream: fs2.Stream[F, Byte]): F[Either[Exception, WidthTable]] = {
    import io.circe.jawn.CirceSupportParser.facade
    import jawnfs2._

    stream.chunks.runJsonOption
      .map {
        case None =>
          new IllegalStateException("The given stream did not contains a JSON").asLeft
        case Some(json) => fromJson(json)
      }
  }

  def fromString(str: String): Either[circe.Error, WidthTable] =
    io.circe.jawn.parse(str).flatMap(fromJson)

  def fromJson(json: Json): Either[DecodingFailure, WidthTable] =
    json.as[Vector[(Long, Long, Double)]].map(WidthTable(_))

  /** Provides an estimate of the given ''text'' based on the given [[WidthTable]].
   *
   * @param table The WidthTable used to know the width of single characters
   * @param text  The text to extract the width of
   * @param guess Whether to use the 'm' char size when the width isn't found
   * @return either the width of the string, or a list of character not found
   */
  def widthOf(
      table: WidthTable,
      text: String,
      guess: Boolean = true): Either[NonEmptyList[NoCharacterWidthFound],
                                     Double] = {
    text.foldLeft(0.0.asRight[NonEmptyList[NoCharacterWidthFound]]) {
      case (Left(nel), char) =>
        table.widthOfCharCode(char) match {
          case None if !guess => Left(NoCharacterWidthFound(char) :: nel)
          case _ => Left(nel)
        }

      case (Right(acc), char) =>
        table.widthOfCharCode(char) match {
          case Some(value) =>
            (acc + value).asRight
          case None =>
            if (guess) (acc + table.emWidth).asRight
            else Left(NonEmptyList.of(NoCharacterWidthFound(char)))
        }
    }
  }

  case class NoCharacterWidthFound(char: Char)
      extends Throwable(s"No width available for character code ${char.toInt}")

}
