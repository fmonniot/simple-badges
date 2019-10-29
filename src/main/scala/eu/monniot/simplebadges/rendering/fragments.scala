package eu.monniot.simplebadges.rendering

import eu.monniot.simplebadges.characters.WidthTable

object fragments {
  def renderLogo(logo: String,
                 logoWidth: Option[Int] = None,
                 logoPadding: Option[Int] = None,
                 extraPadding: Option[Int] = None): (String, Int) = {
    val x = 5 + extraPadding.getOrElse(0)
    val y = 3 + extraPadding.getOrElse(0)

    val width = logoWidth.getOrElse(14)
    val padding = logoPadding.getOrElse(0)

    (
      s"""        <image x="$x" y="$y" width="$width" height="14" xlink:href="${utils.escapeXml(logo)}"/>""",
      width + padding
    )

  }

  def renderTextWithShadow(table: WidthTable,
                           content: String,
                           leftMargin: Int = 0,
                           horizontalPadding: Int = 0,
                           verticalMargin: Int = 0
                          ): (String, Int) = {
    val escapedContent = utils.escapeXml(content).getOrElse("")
    val textLength = utils.preferredWidthOf(table, escapedContent) // Missing 10pt

    val shadowMargin = 150 + verticalMargin
    val textMargin = 140 + verticalMargin

    val outTextLength = 10 * textLength
    val x = (10 * (leftMargin + 0.5 * textLength + 0.5 * horizontalPadding)).toInt

    (
      s"""        <text x="$x" y="$shadowMargin" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="$outTextLength" lengthAdjust="spacing">$escapedContent</text>
         |        <text x="$x" y="$textMargin" transform="scale(.1)" textLength="$outTextLength" lengthAdjust="spacing">$escapedContent</text>""".stripMargin,
      textLength
    )
  }

}
