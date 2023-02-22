package pages

import pages.behaviours.PageBehaviours


class AddTeamMemberDetailsPageSpec extends PageBehaviours {

  "AddTeamMemberDetailsPage" - {

    beRetrievable[String](AddTeamMemberDetailsPage)

    beSettable[String](AddTeamMemberDetailsPage)

    beRemovable[String](AddTeamMemberDetailsPage)
  }
}
