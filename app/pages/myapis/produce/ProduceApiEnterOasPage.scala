package pages.myapis.produce

import pages.QuestionPage
import play.api.libs.json.JsPath

case object ProduceApiEnterOasPage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "produceApiEnterOas"
}
