package eu.monniot.simplebadges.rendering

import cats.implicits._
import org.specs2.Specification
import org.specs2.matcher.XmlMatchers

import Color._

//noinspection TypeAnnotation
class BadgesSpec extends Specification with XmlMatchers {

  override def is =
    s2"""
        `badges.flat` render some known badge
            Can render a 'maven|v2.8.5' badge    $e1
            Can render a '2.8.5' badge           $e2
            Can render a 'logo|style|flatt badge $e3
            Can render with custom label color   $e4
            Can render with custom message color $e5
      """

  import eu.monniot.simplebadges.characters.WidthTableSpec.unsafeVerdanaTable

  def e1 =
    badges.flat(unsafeVerdanaTable, message = "v2.8.5", label = Some("maven")) must beEqualToIgnoringSpace(
      mavenV285Badge())

  def e2 =
    badges.flat(unsafeVerdanaTable, message = "v2.8.5") must beEqualToIgnoringSpace(
      v285Badge)

  def e3 =
    skipped {
      badges.flat(
        unsafeVerdanaTable,
        message = "flat",
        label = Some("style"),
        logo = Some(logo)) must beEqualToIgnoringSpace(styleFlatLogoBadge)
    }

  def e4 =
    badges.flat(
      unsafeVerdanaTable,
      message = "v2.8.5",
      label = Some("maven"),
      labelColor = Some(color"#999")) must beEqualToIgnoringSpace(
      mavenV285Badge(lblColor = color"#999"))

  def e5 =
    badges.flat(
      unsafeVerdanaTable,
      message = "v2.8.5",
      label = Some("maven"),
      messageColor = Some(color"#999")) must beEqualToIgnoringSpace(
      mavenV285Badge(msgColor = color"#999"))

  // Fixtures

  def mavenV285Badge(msgColor: Color = color"#4c1",
                     lblColor: Color = color"#555"): xml.Elem =
    <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="92" height="20">
      <linearGradient id="smooth" x2="0" y2="100%">
        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
        <stop offset="1" stop-opacity=".1"/>
      </linearGradient>

      <clipPath id="roundcorner">
        <rect width="92" height="20" rx="3" fill="#fff"/>
      </clipPath>

      <g clip-path="url(#roundcorner)">
        <rect width="47" height="20" fill={lblColor.show}/>
        <rect x="47" width="45" height="20" fill={msgColor.show}/>
        <rect width="92" height="20" fill="url(#smooth)"/>
      </g>

      <g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="110">

        <text x="245" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="370" lengthAdjust="spacing">maven</text>
        <text x="245" y="140" transform="scale(.1)" textLength="370" lengthAdjust="spacing">maven</text>

        <text x="685" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="350" lengthAdjust="spacing">v2.8.5</text>
        <text x="685" y="140" transform="scale(.1)" textLength="350" lengthAdjust="spacing">v2.8.5</text>
      </g>
    </svg>

  val v285Badge: xml.Elem = {
    <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="45" height="20">
      <linearGradient id="smooth" x2="0" y2="100%">
        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
        <stop offset="1" stop-opacity=".1"/>
      </linearGradient>

      <clipPath id="roundcorner">
        <rect width="45" height="20" rx="3" fill="#fff"/>
      </clipPath>

      <g clip-path="url(#roundcorner)">
        <rect width="0" height="20" fill="#4c1"/>
        <rect x="0" width="45" height="20" fill="#4c1"/>
        <rect width="45" height="20" fill="url(#smooth)"/>
      </g>

      <g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="110">
        <text x="215" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="350" lengthAdjust="spacing">v2.8.5</text>
        <text x="215" y="140" transform="scale(.1)" textLength="350" lengthAdjust="spacing">v2.8.5</text>
      </g>
    </svg>
  }

  val logo: String =
    """data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjMDBCM0UwIiByb2xlPSJpbWciIHZpZXdCb3g9IjAgMCAyNCAyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48dGl0bGU+QXBwVmV5b3IgaWNvbjwvdGl0bGU+PHBhdGggZD0iTSAxMiwwIEMgMTguNiwwIDI0LDUuNCAyNCwxMiAyNCwxOC42IDE4LjYsMjQgMTIsMjQgNS40LDI0IDAsMTguNiAwLDEyIDAsNS40IDUuNCwwIDEyLDAgWiBtIDIuOTQsMTQuMzQgQyAxNi4yNiwxMi42NiAxNi4wOCwxMC4yNiAxNC40LDkgMTIuNzgsNy43NCAxMC4zOCw4LjA0IDksOS43MiA3LjY4LDExLjQgNy44NiwxMy44IDkuNTQsMTUuMDYgYyAxLjY4LDEuMjYgNC4wOCwwLjk2IDUuNCwtMC43MiB6IG0gLTYuNDIsNy44IGMgMC43MiwwLjMgMi4yOCwwLjYgMy4wNiwwLjYgbCA1LjIyLC03LjU2IGMgMS42OCwtMi41MiAxLjI2LC01Ljk0IC0xLjA4LC03LjggLTIuMSwtMS42OCAtNS4wNCwtMS42MiAtNy4xNCwwIGwgLTcuMjYsNS41OCBjIDAuMTgsMS45MiAwLjcyLDIuODggMC43MiwyLjk0IGwgNC4xNCwtNC41IGMgLTAuMywxLjk4IDAuNDIsNC4wMiAyLjEsNS4yOCAxLjQ0LDEuMTQgMy4xOCwxLjQ0IDQuODYsMS4wOCB6Ii8+PC9zdmc+"""

  val styleFlatLogoBadge: xml.Elem = {
    <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="81" height="20">
      <linearGradient id="b" x2="0" y2="100%">
        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
        <stop offset="1" stop-opacity=".1"/>
      </linearGradient>
      <clipPath id="a">
        <rect width="81" height="20" rx="3" fill="#fff"/>
      </clipPath>
      <g clip-path="url(#a)">
        <path fill="#555" d="M0 0h54v20H0z"/>
        <path fill="#97ca00" d="M54 0h27v20H54z"/>
        <path fill="url(#b)" d="M0 0h81v20H0z"/>
      </g>
      <g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="110">
        <image x="5" y="3" width="14" height="14" xlink:href="$logo"/>
        <text x="365" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="270">style</text>
        <text x="365" y="140" transform="scale(.1)" textLength="270">style</text>
        <text x="665" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="170">flat</text>
        <text x="665" y="140" transform="scale(.1)" textLength="170">flat</text>
      </g>
    </svg>
  }

}
