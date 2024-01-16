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

import org.commonmark.ext.gfm.tables.{TableBlock, TableCell, TableHead, TableRow, TablesExtension}
import org.commonmark.node.{AbstractVisitor, BulletList, Heading, Node, Paragraph}
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.{AttributeProvider, AttributeProviderContext, AttributeProviderFactory, HtmlRenderer}
import play.twirl.api.Html

import java.util

object MarkdownUtils {

  // See https://jira.tools.tax.service.gov.uk/browse/HIPP-443
  // See https://github.com/commonmark/commonmark-java

  // The markdown needs to fit into our document structure. The Summary or
  // Description headings are h3's. Therefore the minimum level we can have
  // in the markdown is a 4. HTML supports h6 as the maximum heading level.
  private val minHeaderLevel = 4
  private val maxHeaderLevel = 6

  // Extension to support GitHub markdown's pipe tables
  private val extensions = java.util.Arrays.asList(TablesExtension.create)

  // This parses the markdown text into a node structure
  private val parser = Parser
    .builder()
    .extensions(extensions)
    .build()

  // This renders the node structure into HTML
  private val renderer = HtmlRenderer
    .builder()
    .extensions(extensions)
    .attributeProviderFactory(GovukAttributeProviderFactory)
    .build()

  def parse(markdown: String): Html = {
    val document = parser.parse(markdown)
    document.accept(HeaderVisitor)
    Html(renderer.render(document))
  }

  // This adds the Govuk classes on a per-tag basis.
  // We should probably compare the styles supported by CommonMark and Govuk
  // and add more cases.
  private object GovukAttributeProviderFactory extends AttributeProviderFactory {
    override def create(context: AttributeProviderContext): AttributeProvider = {
      (node: Node, _: String, attributes: util.Map[String, String]) => {
        node match {
          case _: BulletList => attributes.put("class", "govuk-list govuk-list--bullet")
          case _: Paragraph => attributes.put("class", "govuk-body")
          case _: TableBlock => attributes.put("class", "govuk-table")
          case _: TableCell => attributes.put("class", "govuk-table__cell")
          case _: TableHead => attributes.put("class", "govuk-table__head")
          case _: TableRow => attributes.put("class", "govuk-table__row")
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

  // This is some test markdown referenced by the Jira ticket. It comes from a
  // sample OAS file:
  //   https://github.com/hmrc/integration-catalogue-oas-files/blob/master/platforms/test-files/apis/test-api-004-markdown.yaml
  val exampleMarkdown: String =
    """
      |This is a test API to test the rendering of OAS in the API catalogue.
      |This is testing:
      |- Markdown
      |
      |TESTTAG (this is just so you can search for all the test APIs)
      |
      |# Markdown Test
      |
      |## Basic formatting
      |*Bold*
      |
      |_italic_
      |
      |***bold italic***
      |
      |--strike through--
      |
      |## Link
      |a link to <a href="www.google.com">google</a>.
      |
      |## Horizontal lines (different syntax) x 3
      |***
      |---
      |___
      |
      |## Unformatted code
      |### Block ticks
      |```
      |{
      |  "name" : "Dave"
      |}
      |```
      |### Indented
      |    {
      |      "name" : "Dave"
      |    }
      |
      |Word ```code block```.
      |
      |## Headings (remember they get re-levelled to start at 4)
      |
      |# Heading 1
      |## Heading 2
      |### Heading 3
      |#### Heading 4
      |##### Heading 5
      |###### Heading 6
      |####### Heading 7 - invalid
      |
      |Another heading 1
      |=================
      |
      |Another heading 2
      |-----------------
      |
      |## Bullet List
      |- List 1
      |- List 2
      |
      |* List 1
      |++
      |Sub list 1? TODO
      |++
      |* List 2
      |
      |## Number List
      |1. Number List 1
      |1. Number List 2
      |
      |## Images
      |
      |![foo *bar*]
      |
      |[foo *bar*]: https://www.placecage.com/140/100 "train & tracks"
      |
      |Note: Image can't be hosted in the API catalogue
      |
      |# End of OAS description markdown
      |""".stripMargin

}
