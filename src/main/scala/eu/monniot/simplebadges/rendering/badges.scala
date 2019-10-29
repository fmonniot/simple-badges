package eu.monniot.simplebadges.rendering

import cats.implicits._
import eu.monniot.simplebadges.characters.WidthTable

object badges {

  // TODO Add support for links
  def flat(
      table: WidthTable,
      message: String,
      label: Option[String] = None,
      logo: Option[String] = None,
      logoWidth: Option[Int] = None,
      logoPadding: Option[Int] = None, // Might be simple Int with default at 0
      labelColo: String = "#555",
      messageColor: String = "#4c1"): String = {
    val height = 20
    val hasLabel = label.isDefined

    val labelColor: String = if (hasLabel) labelColo else messageColor

    // TODO Validate the colors (probably a newtype)

    val horizontalPadding = 5
    val lWidth = logoWidth.getOrElse(0)
    val lPadding = logoPadding.getOrElse(0)

    val (renderedLabel, labelWidth) =
      label
        .map { l =>
          fragments
            .renderTextWithShadow(
              table,
              content = l,
              leftMargin = (lWidth + lPadding) / 2 + 1,
              horizontalPadding = horizontalPadding * 2
            )
            .map(_ + 2 * horizontalPadding)
        }
        .getOrElse("", 0)

    val (renderedMessage, messageWidth) =
      fragments
        .renderTextWithShadow(
          table,
          content = message,
          leftMargin = (lWidth + lPadding) / 2 + horizontalPadding + labelWidth - (if (message.nonEmpty)
                                                                                     1
                                                                                   else
                                                                                     0),
        )
        .map(_ + 2 * horizontalPadding)

    val leftWidth = labelWidth
    val width = labelWidth + messageWidth

    // TODO Move to scalaxml (with the xml"" interpolator) ?
    s"""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="$width" height="$height">
       |    <linearGradient id="smooth" x2="0" y2="100%">
       |        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
       |        <stop offset="1" stop-opacity=".1"/>
       |    </linearGradient>
       |
       |    <clipPath id="roundcorner">
       |        <rect width="$width" height="$height" rx="3" fill="#fff"/>
       |    </clipPath>
       |
       |    <g clip-path="url(#roundcorner)">
       |        <rect width="$leftWidth" height="$height" fill="$labelColor" />
       |        <rect x="$leftWidth" width="$messageWidth" height="$height" fill="$messageColor" />
       |        <rect width="$width" height="$height" fill="url(#smooth)" />
       |    </g>
       |
       |    <g fill="#fff" text-anchor="middle" ${utils.FontFamily} font-size="110">
       |${logo.map(fragments.renderLogo(_, logoWidth)._1).getOrElse("")}
       |$renderedLabel
       |
       |$renderedMessage
       |    </g>
       |</svg>""".stripMargin
  }

}
