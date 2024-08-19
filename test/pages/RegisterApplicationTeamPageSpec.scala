package pages

import models.RegisterApplicationTeam
import pages.behaviours.PageBehaviours

class RegisterApplicationTeamSpec extends PageBehaviours {

  "RegisterApplicationTeamPage" - {

    beRetrievable[RegisterApplicationTeam](RegisterApplicationTeamPage)

    beSettable[RegisterApplicationTeam](RegisterApplicationTeamPage)

    beRemovable[RegisterApplicationTeam](RegisterApplicationTeamPage)
  }
}
