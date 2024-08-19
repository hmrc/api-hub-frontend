package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait RegisterApplicationTeam

object RegisterApplicationTeam extends Enumerable.Implicits {

  case object Team1 extends WithName("team1") with RegisterApplicationTeam
  case object Team2 extends WithName("team2") with RegisterApplicationTeam

  val values: Seq[RegisterApplicationTeam] = Seq(
    Team1, Team2
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"registerApplicationTeam.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[RegisterApplicationTeam] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
