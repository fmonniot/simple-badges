package eu.monniot.badges.rendering

import eu.monniot.badges.WidthTable

object utils {

  val FontFamily = "DejaVu Sans,Verdana,Geneva,sans-serif"

  def escapeXml(s: String): Option[String] =
    Option(s)
      .filter(_.nonEmpty)
      .map(
        _.replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("\"", "&quot;")
          .replace("'", "&apos;"))

  /** By rounding up values, we can increase the changes of pixel grid alignment
   */
  def roundUpToOdd(value: Int): Int = if (value % 2 == 0) value + 1 else value

  // This method swallow up the widthOf either
  // (which should not contain any error as we are guessing missing widths)
  def preferredWidthOf(table: WidthTable, s: String): Int =
    roundUpToOdd {
      WidthTable
        .widthOf(table, s)
        .map(d => (d / 10).toInt)
        .getOrElse(0)
    }

}
