package pages

import pages.behaviours.PageBehaviours

class ConfirmAddTeamMemberPageSpec extends PageBehaviours {

  "ConfirmAddTeamMemberPage" - {

    beRetrievable[Boolean](ConfirmAddTeamMemberPage)

    beSettable[Boolean](ConfirmAddTeamMemberPage)

    beRemovable[Boolean](ConfirmAddTeamMemberPage)
  }
}
