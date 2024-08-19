package pages

import models.RegisterApplicationTeam
import play.api.libs.json.JsPath

case object RegisterApplicationTeamPage extends QuestionPage[RegisterApplicationTeam] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "registerApplicationTeam"
}
