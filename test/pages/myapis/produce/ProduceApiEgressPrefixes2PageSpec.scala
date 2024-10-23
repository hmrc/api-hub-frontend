package pages.myapis.produce

import pages.behaviours.PageBehaviours

class ProduceApiEgressPrefixesPageSpec extends PageBehaviours {

  "ProduceApiEgressPrefixesPage" - {

    beRetrievable[ProduceApiEgressPrefixes](ProduceApiEgressPrefixesPage)

    beSettable[ProduceApiEgressPrefixes](ProduceApiEgressPrefixesPage)

    beRemovable[ProduceApiEgressPrefixes](ProduceApiEgressPrefixesPage)
  }
}
