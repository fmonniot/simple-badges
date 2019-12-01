/*
 * Copyright 2019 François Monniot
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.monniot.badges.rendering

sealed trait Color

object Color {

  //
  // Data types
  //

  case class HexColor private (hex: String) extends Color
  case class RgbColor private (red: Int,
                               green: Int,
                               blue: Int,
                               alpha: Option[String])
      extends Color
  case class NamedColor private (name: String) extends Color

  //
  // Constructors
  //

  /**
   * Validate that the Hex CSS color is in the format ''#fff'' or ''#fafafa''
   *
   * @param hex the hex string to validate
   * @return the color if valid, or a validation error
   */
  def fromCssHex(hex: String): Either[String, Color] = {
    for {
      _ <- if (hex.length == 4 || hex.length == 7) Right(())
      else
        Left(
          s"Incorrect hex color length: found ${hex.length}, expecting 4 or 7.")
      _ <- if (hex.head == '#') Right(())
      else Left("Hex color didn't start with a '#'")
      _ <- validations.onlyHexCharacters(hex.tail)
    } yield HexColor(hex.tail)
  }

  // Should be doable with a single regex and pattern matching
  def fromCssRgb(rgba: String): Either[String, Color] =
    Left("RGB support not implemented yet")

  def fromCssNamed(name: String): Either[String, Color] =
    if (validations.supportedCssColors.contains(name)) Right(NamedColor(name))
    else Left("Non supported CSS color")

  // We could be smarter here, and look at the first character to decide
  // what constructor to try out (#, rgb, color)
  def fromCss(string: String): Either[String, Color] =
    fromCssHex(string).orElse(fromCssRgb(string)).orElse(fromCssNamed(string))

  // This only exists because there is some part of the code where we can't use the macro
  // (same compilation unit)
  private[badges] def unsafeFromString(string: String): Color =
    fromCss(string).fold(e => throw new RuntimeException(e), identity)

  //
  // Type classes
  //

  implicit val show: cats.Show[Color] = cats.Show.show {
    case HexColor(hex) => s"#$hex"
    case RgbColor(red, green, blue, Some(alpha)) =>
      s"rgba($red, $green, $blue, $alpha)"
    case RgbColor(red, green, blue, None) => s"rgb($red, $green, $blue)"
    case NamedColor(name) => name
  }

  //
  // Utilities
  //
  implicit class ColorLiteral(val sc: StringContext) extends AnyVal {
    def color(args: Any*): Color = macro macros.colorInterpolator
  }

  private object macros {

    import scala.reflect.macros.blackbox

    def colorInterpolator(
        c: blackbox.Context
    )(args: c.Expr[Any]*): c.Expr[Color] = {
      import c.universe._

      // The args parameter is never used but required by the macro contract
      // our scalac config require parameters to be used though, so let's
      identity(args)

      c.prefix.tree match {
        case Apply(
            _,
            List(Apply(_, (lcp @ Literal(Constant(p: String))) :: Nil))
            ) =>
          val valid = Color.fromCss(p)
          valid match {
            case Left(error) =>
              c.abort(
                c.enclosingPosition,
                s"Invalid Color: $error"
              )
            case Right(_) =>
              val value: c.Expr[String] = c.Expr(lcp)
              c.universe.reify(
                Color
                  .fromCss(value.splice)
                  .fold(
                    _ => throw new RuntimeException("Unreachable state"),
                    identity))
          }
      }
    }

  }

  private[rendering] object validations {
    val hexChars: IndexedSeq[Char] = ('a' to 'f') ++ ('0' to '9')

    val supportedCssColors: Set[String] = Set(
      "green",
      "brightgreen",
      "yellowgreen",
      "yellow",
      "orange",
      "red",
      "blue",
      "lightgrey")

    def onlyHexCharacters(string: String): Either[String, Unit] = {
      val invalidCharacters = string.toLowerCase.zipWithIndex
        .collect {
          case (c, i) if !hexChars.contains(c) => s"'$c' at pos $i"
        }

      if (invalidCharacters.isEmpty) Right(())
      else
        Left(invalidCharacters.mkString("Invalid characters found: ", ", ", ""))
    }

    def range(min: Int, max: Int, n: Int): Either[String, Unit] =
      if (n >= min && n <= max) Right(())
      else Left(s"Number $n not in range [$min, $max]")
  }
}
