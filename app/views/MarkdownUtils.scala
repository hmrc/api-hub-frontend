/*
 * Copyright 2023 HM Revenue & Customs
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

package views

import org.commonmark.ext.gfm.tables.{TableBlock, TableBody, TableCell, TableHead, TableRow, TablesExtension}
import org.commonmark.node.{AbstractVisitor, BulletList, Heading, Link, Node, OrderedList, Paragraph, ThematicBreak}
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.{AttributeProvider, AttributeProviderContext, AttributeProviderFactory, HtmlRenderer}
import play.twirl.api.Html

import java.util

object MarkdownUtils {
  // See https://github.com/commonmark/commonmark-java

  // The markdown needs to fit into our document structure. The Summary or
  // Description headings are h3's. Therefore the minimum level we can have
  // in the markdown is a 4. HTML supports h6 as the maximum heading level.
  private val minHeaderLevel = 4
  private val maxHeaderLevel = 6

  // Extension to support GitHub markdown's pipe tables
  // see https://github.github.com/gfm/#tables-extension-
  private val extensions = java.util.Arrays.asList(TablesExtension.create)

  private val parser = Parser
    .builder()
    .extensions(extensions)
    .build()

  private val renderer = HtmlRenderer
    .builder()
    .extensions(extensions)
    .attributeProviderFactory(GovukAttributeProviderFactory)
    .build()

  def parse(markdown: String): Html = {
    // Both parser and renderer are thread-safe https://github.com/commonmark/commonmark-java?tab=readme-ov-file#thread-safety
    val document = parser.parse(markdown)
    document.accept(HeaderVisitor)
    Html(renderer.render(document))
  }

  private object GovukAttributeProviderFactory extends AttributeProviderFactory {
    override def create(context: AttributeProviderContext): AttributeProvider = {
      (node: Node, _: String, attributes: util.Map[String, String]) => {
        node match {
          case _: BulletList => attributes.put("class", "govuk-list govuk-list--bullet")
          case _: OrderedList => attributes.put("class", "govuk-list govuk-list--number")
          case _: Paragraph => attributes.put("class", "govuk-body")
          case _: TableBlock => attributes.put("class", "govuk-table")
          case _: TableHead => attributes.put("class", "govuk-table__head")
          case _: TableBody => attributes.put("class", "govuk-table__body")
          case cell: TableCell if cell.isHeader => attributes.put("class", "govuk-table__header")
          case cell: TableCell if ! cell.isHeader => attributes.put("class", "govuk-table__cell")
          case _: TableRow => attributes.put("class", "govuk-table__row")
          case _: Heading => attributes.put("class", "govuk-heading-s")
          case _: Link => attributes.put("class", "govuk-link")
          case _: ThematicBreak => attributes.put("class", "govuk-section-break govuk-section-break--visible")
          case _ => ()
        }
      }
    }
  }

  // This transforms heading tags:
  //  h1 -> h4
  //  h2 -> h5
  //  h3 -> h6
  //  h4 -> h6 (remember h6 is the maximum)
  //  h5 -> h6
  //  h6 -> h6
  private object HeaderVisitor extends AbstractVisitor {
    override def visit(heading: Heading): Unit = {
      heading.setLevel(Math.min(heading.getLevel + minHeaderLevel - 1, maxHeaderLevel))
    }
  }

}
