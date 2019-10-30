package eu.monniot.simplebadges.rendering

import cats.implicits._
import eu.monniot.simplebadges.characters.WidthTable
import Color._

object badges {

  // TODO Add support for links
  def flat(
      table: WidthTable,
      message: String,
      label: Option[String] = None,
      logo: Option[String] = None,
      logoWidth: Option[Int] = None,
      logoPadding: Option[Int] = None, // Might be simple Int with default at 0
      labelColor: Option[Color] = None,
      messageColor: Option[Color] = None): xml.Elem = {
    val height = 20
    val hasLabel = label.isDefined

    val horizontalPadding = 5
    val loWidth = logoWidth.getOrElse(0)
    val loPadding = logoPadding.getOrElse(0)

    val (renderedLabel, labelWidth) =
      label
        .map { l =>
          fragments
            .renderTextWithShadow(
              table,
              content = l,
              leftMargin = (loWidth + loPadding) / 2 + 1,
              horizontalPadding = horizontalPadding * 2
            )
            .map(_ + 2 * horizontalPadding)
        }
        .getOrElse(Seq.empty -> 0)

    val leftMargin =
      (loWidth + loPadding) / 2 +
        horizontalPadding +
        labelWidth -
        (if (message.nonEmpty) 1 else 0)

    val (renderedMessage, messageWidth) =
      fragments
        .renderTextWithShadow(
          table,
          content = message,
          leftMargin = leftMargin,
        )
        .map(_ + 2 * horizontalPadding)

    val leftWidth = labelWidth
    val width = labelWidth + messageWidth

    val lblColor = labelColor.getOrElse(Color.unsafeFromString("#555")).show
    val msgColor = messageColor.getOrElse(Color.unsafeFromString("#4c1")).show

    <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width={width.toString} height={height.toString}>
      <linearGradient id="smooth" x2="0" y2="100%">
        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
        <stop offset="1" stop-opacity=".1"/>
      </linearGradient>

      <clipPath id="roundcorner">
        <rect width={width.toString} height={height.toString} rx="3" fill="#fff"/>
      </clipPath>

      <g clip-path="url(#roundcorner)">
        <rect width={leftWidth.toString} height={height.toString} fill={if (hasLabel) lblColor else msgColor}/>
        <rect x={leftWidth.toString} width={messageWidth.toString} height={height.toString} fill={msgColor}/>
        <rect width={width.toString} height={height.toString} fill="url(#smooth)"/>
      </g>

      <g fill="#fff" text-anchor="middle" font-family={utils.FontFamily} font-size="110">
        {logo.map(fragments.renderLogo(_, logoWidth)._1).orNull}{renderedLabel}{renderedMessage}
      </g>
    </svg>
  }

}
