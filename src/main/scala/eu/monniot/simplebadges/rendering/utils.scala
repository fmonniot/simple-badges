package eu.monniot.simplebadges.rendering

import eu.monniot.simplebadges.characters.WidthTable


object utils {

  val FontFamily = """font-family="DejaVu Sans,Verdana,Geneva,sans-serif""""

  def escapeXml(s: String): Option[String] =
    Option(s)
      .filter(_.nonEmpty)
      .map(_.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;"))

  /** By rounding up values, we can increase the changes of pixel grid alignment
    */
  def roundUpToOdd(value: Int): Int = if (value % 2 == 0) value + 1 else value


  def preferredWidthOf(table: WidthTable, s: String): Int =
    roundUpToOdd((WidthTable.widthOf(table, s) / 10).toInt)


}
