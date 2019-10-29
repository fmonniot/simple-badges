package eu.monniot.simplebadges.rendering

object TryOne {

  def root(children: List[String],
           widths: (Int, Int),
           texts: (String, String),
           logo: Option[_],
           labelColor: Option[_]) = {
    val (leftWidth, rightWidth) = widths
    val (leftText, _) = texts

    val width: Int = {
      val left =
        if (leftWidth - leftText.length != 0) 0
        else if (logo.isEmpty) 11
        else if (labelColor.isDefined) 0
        else 7

      left + rightWidth
    }

    s"""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="$width" height="20">
       |    ${children.mkString("    ", "\n\n\n    ", "\n")}
       |</svg>""".stripMargin
  }

  def escapeXml(s: String) =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

  def svgTemplate(w: Option[(Int, Int)]): String = {
    val text = ("version", "1.2.3")
    val escapedText = (escapeXml(text._1), escapeXml(text._2))
    val logo: Option[String] = None
    val logoWidth = 0
    val logoPadding = 0
    val textColor: Option[String] = None
    val labelColor: Option[String] = None

    // TODO computed from text
    val widths = w.getOrElse(100 -> 50) // left, right

    val gradient =
      """<linearGradient id="smooth" x2="0" y2="100%">
        |        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
        |        <stop offset="1" stop-opacity=".1"/>
        |    </linearGradient>
        |""".stripMargin

    val clipPath =
      s"""<clipPath id="round">
         |        <rect width="${widths._1 + widths._2}" height="20" rx="3" fill="#fff"/>
         |    </clipPath>
         |""".stripMargin

    // Find better variable name
    val gClipPath = {

      val fill1 =
        if (text._1.length != 0 || logo.isDefined && labelColor.isDefined)
          labelColor.getOrElse("#555")
        else textColor.getOrElse("#4c1")

      s"""<g clip-path="url(#round)">
         |        <rect width="${widths._1}" height="20" fill="${escapeXml(
        fill1
      )}"/>
         |        <rect x="${widths._1}" width="${widths._2}" height="20" fill="${escapeXml(
        textColor.getOrElse("#4c1")
      )}"/>
         |        <rect width="${widths._1 + widths._2}" height="20" fill="url(#smooth)"/>
         |    </g>
         |""".stripMargin
    }

    val mainContent = {

      val logoContent =
        logo match {
          case Some(value) =>
            s"""<image x="5" y="3" width="$logoWidth" height="14" xlink:href="$value"/>"""
          case None => ""
        }

      val leftText = if (text._2.nonEmpty) {
        val x = 10 * (1 + ((widths._1 + logoWidth + logoPadding) / 2))
        val textLength = 10 * (widths._1 - (10 + logoWidth + logoPadding))

        s"""<text x="$x" y="150" fill="#010101" fill-opacity=".3" transform="scale(0.1)" textLength="$textLength" lengthAdjust="spacing">${escapedText._1}</text>
           |        <text x="$x" y="140" transform="scale(0.1)" textLength="$textLength" lengthAdjust="spacing">${escapedText._1}</text>
           |""".stripMargin
      } else ""

      val rightText = {

        val x = 10 * (widths._1 + (widths._2 / 2) - (if (text._1.nonEmpty) 1
        else 0))
        val textLength = (widths._2 - 10) * 10

        s"""<text x="$x" y="150" fill="#010101" fill-opacity=".3" transform="scale(0.1)" textLength="$textLength" lengthAdjust="spacing">${escapedText._2}</text>
           |        <text x="$x" y="140" transform="scale(0.1)" textLength="$textLength" lengthAdjust="spacing">${escapedText._2}</text>
           |""".stripMargin
      }

      s"""<g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="110">
         |        $logoContent
         |        $leftText
         |        $rightText
         |    </g>
         |""".stripMargin
    }

    root(
      List(gradient, clipPath, gClipPath, mainContent),
      widths,
      escapedText,
      logo,
      labelColor
    )
  }

}
