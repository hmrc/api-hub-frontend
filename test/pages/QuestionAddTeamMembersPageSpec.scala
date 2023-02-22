package pages

import pages.behaviours.PageBehaviours

class QuestionAddTeamMembersPageSpec extends PageBehaviours {

  "QuestionAddTeamMembersPage" - {

    beRetrievable[Boolean](QuestionAddTeamMembersPage)

    beSettable[Boolean](QuestionAddTeamMembersPage)

    beRemovable[Boolean](QuestionAddTeamMembersPage)
  }
}
