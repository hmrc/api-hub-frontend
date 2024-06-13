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

import config.DomainsSpec.{Table, forAll}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html


class MarkdownUtilsSpec extends AnyFreeSpec with Matchers {

  def matchTheHtml(txt: String) = new HtmlMatcher(Html(txt))

  "markdown parsing" - {
    "Horizontal rules" - {
      forAll(Table("hrs", "---", "***", "   -----   ")) { markdown =>
        MarkdownUtils.parse(markdown) must matchTheHtml("<hr class=\"govuk-section-break govuk-section-break--visible\" />")
      }
    }
    def h(level: Int) = s"<h$level class=\"govuk-heading-s\">heading</h$level>"

    "Headings" - {
      "ATX Headings" - {
        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("# heading", h(4)),
          ("## heading", h(5)),
          ("### heading", h(6)),
          ("#### heading", h(6)),
          ("##### heading", h(6)),
          ("###### heading", h(6)),
          ("   ###### heading", h(6)),
          ("######     heading     ", h(6)),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }

      "Setext Headings" - {
        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("heading\n===", h(4)),
          ("   heading  \n   ======= ", h(4)),
          ("heading\n---", h(5)),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }
    }

    "Code Blocks" - {
      def code(lines: String*) = s"<pre><code>${lines.mkString("\n")}\n</code></pre>"

      "Indented" - {
        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("    val a=1", code("val a=1")),
          ("""    val a=1
             |    val b=2""".stripMargin, code("val a=1", "val b=2")),
          ("""    val a=1
             |    val b=2
             |      val c=3""".stripMargin, code("val a=1", "val b=2", "  val c=3")),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }

      "Fenced" - {
        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("""```
             |val a=1
             |```""".stripMargin, code("val a=1")),
          ("""```
             |val a=1
             |  val b=2
             |```""".stripMargin, code("val a=1", "  val b=2")),
          ("""~~~
             |val a=1
             |  val b=2
             |~~~""".stripMargin, code("val a=1", "  val b=2")),
          ("""   ~~~~~
             |val a=1
             |val b=2
             |  ~~~~~~~~""".stripMargin, code("val a=1", "val b=2")),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }
    }

    "HTML" - {
      forAll(Table("HTML", "<p></p>", "<div class=\"govuk-body\"></div>", "<p>\n</div>")) { html =>
        MarkdownUtils.parse(html) must matchTheHtml(html)
      }
    }

    def p(content: String) = s"<p class=\"govuk-body\">$content</p>\n"
    "Paragraphs" - {
      forAll(Table(
        ("Markdown", "Expected HTML"),
        ("a", p("a")),
        ("a\nb\nc", p("a\nb\nc")),
        ("a\n\nb\n\nc", p("a") + p("b") + p("c"))
      )) { (markdown, expectedHtml) =>
        MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
      }
    }

    "Blockquotes" - {
      def quote(content: String) = s"<blockquote>\n$content</blockquote>\n"

      forAll(Table(
        ("Markdown", "Expected HTML"),
        ("> a", quote(p("a"))),
        ("   >a", quote(p("a"))),
        ("> # heading\n> text", quote(h(4) + "\n" + p("text"))),
        ("> a\n> b", quote(p("a\nb"))),
        ("> a\n\n> b", quote(p("a")) + quote(p("b"))),
      )) { (markdown, expectedHtml) =>
        MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
      }
    }

    "Lists" - {
      "Unordered" - {
        def ul(items: String*) = s"<ul class=\"govuk-list govuk-list--bullet\">\n${items.map(i => s"<li>$i</li>").mkString("\n")}\n</ul>\n"

        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("- a", ul("a")),
          ("+ a", ul("a")),
          ("* a", ul("a")),
          ("   -    a\n -  b\n- c", ul("a", "b", "c")),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }
      "Ordered" - {
        def ol(items: String*) = s"<ol class=\"govuk-list govuk-list--number\">\n${items.map(i => s"<li>$i</li>").mkString("\n")}\n</ol>\n"

        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("1. a", ol("a")),
          ("1) a", ol("a")),
          ("1. a", ol("a")),
          ("   1.    a\n 1.  b\n2. c", ol("a", "b", "c")),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }
    }

    "Inline formatting" - {
      "code" - {
        def code(content: String) = s"<p class=\"govuk-body\"><code>$content</code></p>"
        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("`val a=1`", code("val a=1")),
          ("``val a=1``", code("val a=1")),
          ("```val a=1```", code("val a=1")),
          ("`val a=1\nval b=2`", code("val a=1 val b=2")),
          ("` val a  =  1  `", code("val a  =  1 ")),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }

      "emphasis" - {
        def p(content: String) = s"<p class=\"govuk-body\">$content</p>"
        def em(content: String) = s"<em>$content</em>"
        def strong(content: String) = s"<strong>$content</strong>"

        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("*italic*", p(em("italic"))),
          ("_italic_", p(em("italic"))),
          ("**bold**", p(strong("bold"))),
          ("__bold__", p(strong("bold"))),
          ("***italic and bold***", p(em(strong("italic and bold")))),
          ("___italic and bold___", p(em(strong("italic and bold")))),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }

      "links" - {
        def a(url: String, text: String) = s"<p class=\"govuk-body\"><a href=\"$url\" class=\"govuk-link\">$text</a></p>"
        def aWithTitle(url: String, text: String, title: String) = s"<p class=\"govuk-body\"><a href=\"$url\" title=\"$title\" class=\"govuk-link\">$text</a></p>"

        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("[Google](http://google.com)", a("http://google.com", "Google")),
          ("[Home](/home)", a("/home", "Home")),
          ("[Google](  http://google.com   )", a("http://google.com", "Google")),
          ("[Google](http://google.com \"Search here\")", aWithTitle("http://google.com", "Google", "Search here")),
          ("<http://google.com>", a("http://google.com", "http://google.com")),
          ("<user@hmrc.gov.uk>", a("mailto:user@hmrc.gov.uk", "user@hmrc.gov.uk")),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }

      "images" - {
        def img(url: String, altText: String) = s"<p class=\"govuk-body\"><img src=\"$url\" alt=\"$altText\" /></p>"

        forAll(Table(
          ("Markdown", "Expected HTML"),
          ("![Snoopy](http://snoopy.com/main.jpg)", img("http://snoopy.com/main.jpg", "Snoopy")),
          ("![Snoopy](/main.jpg)", img("/main.jpg", "Snoopy")),
          ("![Snoopy](   http://snoopy.com/main.jpg   )", img("http://snoopy.com/main.jpg", "Snoopy")),
        )) { (markdown, expectedHtml) =>
          MarkdownUtils.parse(markdown) must matchTheHtml(expectedHtml)
        }
      }

      "tables" - {
        val tableHtml =
          """
            |<table class="govuk-table">
            |<thead class="govuk-table__head">
            |<tr class="govuk-table__row">
            |<th class="govuk-table__header">foo</th>
            |<th class="govuk-table__header">bar</th>
            |</tr>
            |</thead>
            |<tbody class="govuk-table__body">
            |<tr class="govuk-table__row">
            |<td class="govuk-table__cell">baz</td>
            |<td class="govuk-table__cell">bim</td>
            |</tr>
            |</tbody>
            |</table>
            |""".stripMargin

        forAll(Table(
          """| foo | bar |
              | --- | --- |
              | baz | bim |""".stripMargin,
          """|   foo | bar    |
             |--- | -----------
             |baz | bim  """.stripMargin)) { markdown: String =>
          MarkdownUtils.parse(markdown) must matchTheHtml(tableHtml)
        }
      }
    }
  }

}

class HtmlMatcher(expectedHtml: Html) extends Matcher[Html] {
  override def apply(actualHtml: Html): MatchResult = {
    val actual = actualHtml.toString.trim
    val expected = expectedHtml.toString.trim
    MatchResult(actual.equals(expected), s"rendered HTML ($actual) does not match the expected value of ($expected)", "html is ok valid")
  }
}


