package pages

import models.ProduceApiHowToCreate
import pages.behaviours.PageBehaviours
import pages.myapis.produce.ProduceApiHowToCreatePage

class ProduceApiHowToCreateSpec extends PageBehaviours {

  "ProduceApiHowToCreatePage" - {

    beRetrievable[ProduceApiHowToCreate](ProduceApiHowToCreatePage)

    beSettable[ProduceApiHowToCreate](ProduceApiHowToCreatePage)

    beRemovable[ProduceApiHowToCreate](ProduceApiHowToCreatePage)
  }
}
