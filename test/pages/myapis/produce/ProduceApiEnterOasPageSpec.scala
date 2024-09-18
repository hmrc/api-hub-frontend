package pages.myapis.produce

import pages.behaviours.PageBehaviours
import pages.myapis.produce.ProduceApiEnterOasPage


class ProduceApiEnterOasPageSpec extends PageBehaviours {

  "ProduceApiEnterOasPage" - {

    beRetrievable[String](ProduceApiEnterOasPage)

    beSettable[String](ProduceApiEnterOasPage)

    beRemovable[String](ProduceApiEnterOasPage)
  }
}
