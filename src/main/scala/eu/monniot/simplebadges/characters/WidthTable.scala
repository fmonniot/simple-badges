package eu.monniot.simplebadges.characters

import cats.implicits._

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

  // TODO Error management + extract I/O loading from here
  def load: WidthTable = {
    val text = scala.io.Source.fromResource("verdana-table.json").mkString
    val json = io.circe.jawn.parse(text).getOrElse(null)

    val vec = json.asArray.get.map(_.as[(Long, Long, Double)].getOrElse(null))

    WidthTable(vec)
  }

  // TODO Remove the exception and wrap in an Either[String, Double]
  def widthOf(table: WidthTable, text: String, guess: Boolean = true): Double =
    text.foldLeft(0.0) {
      case (acc, char) =>
        table.widthOfCharCode(char) match {
          case Some(value) =>
            acc + value
          case None =>
            if (guess) acc + table.emWidth
            else
              throw new IllegalArgumentException(
                s"No width available for character code ${char.toInt}")
        }
    }

}
