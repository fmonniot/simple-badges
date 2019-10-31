package eu.monniot.badges

import cats.Show
import eu.monniot.badges.rendering.Color
import org.specs2.Specification
import org.specs2.execute.Typecheck._
import org.specs2.matcher.TypecheckMatchers._

//noinspection TypeAnnotation
class ColorSpec extends Specification {

  import Color._

  def is =
    s2"""
  This is a specification to check the Color data type.

  A color can be constructed using string literal and a string interpolator
    it will fail at compile time if the color is malformed $inter1
    it will offer a Color directly otherwise               $inter2

  Color#fromCssHex let build a color based on a css hex string
    it will work with regular hex string                          $hex1
    it will also work with simplified hex string                  $hex2
    the hex string needs to have the correct amount of characters $hex3
    the hex string must start with a pound                        $hex4
    the hex string must contains only hexa characters             $hex5

  Color#fromCssNamed let build a color based on a css predefined color
    it will work with whitelisted colors $named1
    and failed with any other string     $named2

  Color#fromCss is a special combinator which try the three others in order
    it will work with hex color   $css1
    it will work with named color $css2
    it will fail otherwise        $css3
    
  Color also defines a Show type class which render the CSS color property
    with a hex based color   $show1
    with a name based color  $show2
    """

  def inter1 = typecheck("""color"notacolor"""") must failWith(
    "Invalid Color: Non supported CSS color"
  )

  def inter2 = color"#aaa" must_=== Color.fromCssHex("#aaa").getOrElse(null)

  def hex1 = Color.fromCssHex("#ababab") must beRight(color"#ababab")
  def hex2 = Color.fromCssHex("#aaa") must beRight(color"#aaa")

  def hex3 =
    Color.fromCssHex("#abab") must beLeft(
      "Incorrect hex color length: found 5, expecting 4 or 7.")

  def hex4 =
    Color.fromCssHex("aaaa") must beLeft("Hex color didn't start with a '#'")

  def hex5 =
    Color.fromCssHex("#abzcfw") must beLeft(
      "Invalid characters found: 'z' at pos 2, 'w' at pos 5")

  def named1 = Color.fromCssNamed("green") must beRight(color"green")

  def named2 =
    Color.fromCssNamed("notacolor") must beLeft("Non supported CSS color")

  def css1 = Color.fromCss("#aaa") must beRight(color"#aaa")
  def css2 = Color.fromCss("green") must beRight(color"green")
  def css3 = Color.fromCss("blahblah") must beLeft("Non supported CSS color")

  def show1 = Show[Color].show(color"#aaa") must_=== "#aaa"
  def show2 = Show[Color].show(color"green") must_=== "green"
}
