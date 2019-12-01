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

import eu.monniot.badges.WidthTable

object fragments {

  def renderLogo(logo: String,
                 logoWidth: Option[Int] = None,
                 logoPadding: Option[Int] = None,
                 extraPadding: Option[Int] = None): (xml.Elem, Int) = {
    val x = 5 + extraPadding.getOrElse(0)
    val y = 3 + extraPadding.getOrElse(0)

    val width = logoWidth.getOrElse(14)
    val padding = logoPadding.getOrElse(0)

    // The orNull trick for not assigning an attribute may be problematic. Let's wait and see on that one
    (
      <image x={x.toString} y={y.toString} width={width.toString} height="14" xlink:href={utils.escapeXml(logo).orNull}/>,
      width + padding
    )

  }

  def renderTextWithShadow(table: WidthTable,
                           content: String,
                           leftMargin: Int = 0,
                           horizontalPadding: Int = 0,
                           verticalMargin: Int = 0): (Seq[xml.Elem], Int) = {
    val escapedContent = utils.escapeXml(content).getOrElse("")
    val textLength = utils.preferredWidthOf(table, escapedContent)

    val shadowMargin = 150 + verticalMargin
    val textMargin = 140 + verticalMargin

    val outTextLength = 10 * textLength
    val x =
      (10 * (leftMargin + 0.5 * textLength + 0.5 * horizontalPadding)).toInt

    (
      Seq(
        <text x={x.toString} y={shadowMargin.toString} fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength={outTextLength.toString} lengthAdjust="spacing">
          {escapedContent}
        </text>,
        <text x={x.toString} y={textMargin.toString} transform="scale(.1)" textLength={outTextLength.toString} lengthAdjust="spacing">
          {escapedContent}
        </text>
      ),
      textLength
    )
  }

}
