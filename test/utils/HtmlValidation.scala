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

package utils

import nu.validator.messages.{MessageEmitterAdapter, TextMessageEmitter}
import nu.validator.servlet.imagereview.ImageCollector
import nu.validator.source.SourceCode
import nu.validator.validation.SimpleDocumentValidator
import nu.validator.xml.SystemErrErrorHandler
import org.scalatest.matchers.{MatchResult, Matcher}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import scala.xml.InputSource

trait HtmlValidation {

  class StringIsValidHtmlMatcher extends Matcher[String] {
    override def apply(html: String): MatchResult = {
      // Boilerplate
      val sourceCode = new SourceCode()
      val imageCollector = new ImageCollector(sourceCode)
      val out = new ByteArrayOutputStream()
      val emitter = new TextMessageEmitter(out, false)
      val errorHandler = new MessageEmitterAdapter(null, sourceCode, false, imageCollector, 0, false, emitter)

      // Whether we get output for errors only or errors and warnings
      errorHandler.setErrorsOnly(false)

      val validator = new SimpleDocumentValidator(true, true, false)

      // Other schemas are available
      // This is an identifier not a URL which will be used to fetch anything
      validator.setUpMainSchema("http://s.validator.nu/html5-rdfalite.rnc", new SystemErrErrorHandler())

      validator.setUpValidatorAndParsers(errorHandler, true, false)

      // Perform the validation
      validator.checkHtmlInputSource(
        new InputSource(
          new ByteArrayInputStream(html.getBytes("UTF-8"))
        )
      )

      // Need to do this or you get no output
      errorHandler.end("HTML is valid", "HTML fails validation", null)

      // We now have output
      val output = out.toString("UTF-8")

      MatchResult(
        errorHandler.getFatalErrors + errorHandler.getErrors + errorHandler.getWarnings == 0,
        s"HTML has ${errorHandler.getErrors} errors and ${errorHandler.getWarnings} warnings ${System.lineSeparator()}$output",
        output
      )
    }
  }

  def validateAsHtml = new StringIsValidHtmlMatcher

}
