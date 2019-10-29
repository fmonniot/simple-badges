package eu.monniot.simplebadges.rendering

import eu.monniot.simplebadges.characters.WidthTable
import org.specs2.Specification

class BadgesSpec extends Specification {

  override def is =
    s2"""
        badges.flat render some known badge
            Can render a 'maven|v2.8.5' badge $e1
            Can render a '2.8.5' badge        $e2
            TODO With a log in front
            TODO With custom label color
            TODO With custom message color
      """


  def e1 = badges.flat(WidthTable.load, message = "v2.8.5", label = Some("maven")) must beEqualTo(mavenV285Badge)
  def e2 = skipped(badges.flat(WidthTable.load, message = "v2.8.5") must beEqualTo(mavenV285Badge))


  // Fixtures

  val mavenV285Badge: String =
    """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="92" height="20">
      |    <linearGradient id="smooth" x2="0" y2="100%">
      |        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
      |        <stop offset="1" stop-opacity=".1"/>
      |    </linearGradient>
      |
      |    <clipPath id="roundcorner">
      |        <rect width="92" height="20" rx="3" fill="#fff"/>
      |    </clipPath>
      |
      |    <g clip-path="url(#roundcorner)">
      |        <rect width="47" height="20" fill="#555" />
      |        <rect x="47" width="45" height="20" fill="#4c1" />
      |        <rect width="92" height="20" fill="url(#smooth)" />
      |    </g>
      |
      |    <g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="110">
      |
      |        <text x="245" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="370" lengthAdjust="spacing">maven</text>
      |        <text x="245" y="140" transform="scale(.1)" textLength="370" lengthAdjust="spacing">maven</text>
      |
      |        <text x="685" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="350" lengthAdjust="spacing">v2.8.5</text>
      |        <text x="685" y="140" transform="scale(.1)" textLength="350" lengthAdjust="spacing">v2.8.5</text>
      |    </g>
      |</svg>""".stripMargin

}
