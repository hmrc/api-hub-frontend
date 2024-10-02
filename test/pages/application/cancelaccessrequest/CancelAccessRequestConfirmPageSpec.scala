package pages.application.cancelaccessrequest

import pages.application.cancelaccessrequest.CancelAccessRequestConfirmPage
import pages.behaviours.PageBehaviours

class CancelAccessRequestConfirmPageSpec extends PageBehaviours {

  "CancelAccessRequestConfirmPage" - {

    beRetrievable[Boolean](CancelAccessRequestConfirmPage)

    beSettable[Boolean](CancelAccessRequestConfirmPage)

    beRemovable[Boolean](CancelAccessRequestConfirmPage)
  }
}
