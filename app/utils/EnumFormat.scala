/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.*

import scala.quoted.*

trait EnumFormat[T] extends Format[T]

object EnumMacros {

  def enumFormatMacro[T: Type](using Quotes): Expr[EnumFormat[T]] = {
    import quotes.reflect.*
    val tpe = TypeRepr.of[T]

    val sym = tpe.classSymbol match {
      case Some(sym)
        if sym.flags.is(Flags.Enum) && !sym.flags.is(Flags.JavaDefined) =>
        sym
      case _ =>
        report.errorAndAbort(s"${tpe.show} is not an enum type")
    }

    def reifyValueOf(name: Expr[String]) =
      Select
        .overloaded(Ref(sym.companionModule), "valueOf", Nil, name.asTerm :: Nil)
        .asExprOf[T & reflect.Enum]

    '{
      new EnumFormat[T] {
        private def valueOfUnsafe(name: String): T = ${
          reifyValueOf('name)
        }

        override def reads(json: JsValue): JsResult[T] = try {
          JsSuccess(valueOfUnsafe(json.as[JsString].value))
        } catch {
          case e: NoSuchElementException => JsError(e.getMessage)
        }

        override def writes(o: T): JsValue = JsString(o.toString)
      }
    }
  }
}

object EnumFormat:
  inline def derived[T]: EnumFormat[T] = ${ EnumMacros.enumFormatMacro[T] }
