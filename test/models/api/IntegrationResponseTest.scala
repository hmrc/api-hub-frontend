/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.api

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class IntegrationResponseTest extends AnyFreeSpec with Matchers {

  "IntegrationResponse" - {
    "should not include the openApiSpecification" in {

      val json =
        """
          |{
          |  "count": 1,
          |  "pagedCount": 1,
          |  "results": [
          |    {
          |      "maintainer": {
          |        "name": "name",
          |        "slackChannel": "#slack",
          |        "contactInfo": []
          |      },
          |      "version": "1.0.0",
          |      "id": "id1",
          |      "apiStatus": "LIVE",
          |      "reviewedDate": "2024-10-10T09:56:23.385+0000",
          |      "title": "zebras",
          |      "description": "zebras api",
          |      "endpoints": [],
          |      "platform": "HIP",
          |      "hods": [],
          |      "publisherReference": "ref1",
          |      "openApiSpecification": "oas",
          |      "lastUpdated": "2024-10-10T09:56:23.385+0000"
          |    },
          |    {
          |      "maintainer": {
          |        "name": "name",
          |        "slackChannel": "#slack",
          |        "contactInfo": []
          |      },
          |      "version": "1.0.0",
          |      "id": "id2",
          |      "apiStatus": "LIVE",
          |      "reviewedDate": "2024-10-10T09:56:23.385+0000",
          |      "title": "zebras",
          |      "description": "zebras api",
          |      "endpoints": [],
          |      "platform": "HIP",
          |      "hods": [],
          |      "publisherReference": "ref2",
          |      "openApiSpecification": "oas",
          |      "lastUpdated": "2024-10-10T09:56:23.385+0000"
          |    },
          |    {
          |      "maintainer": {
          |        "name": "name",
          |        "slackChannel": "#slack",
          |        "contactInfo": []
          |      },
          |      "version": "1.0.0",
          |      "id": "id3",
          |      "apiStatus": "LIVE",
          |      "reviewedDate": "2024-10-10T09:56:23.385+0000",
          |      "title": "zebras",
          |      "description": "zebras api",
          |      "endpoints": [],
          |      "platform": "HIP",
          |      "hods": [],
          |      "publisherReference": "ref3",
          |      "openApiSpecification": "oas",
          |      "lastUpdated": "2024-10-10T09:56:23.385+0000"
          |    },
          |    {
          |      "maintainer": {
          |        "name": "name",
          |        "slackChannel": "#slack",
          |        "contactInfo": []
          |      },
          |      "version": "1.0.0",
          |      "id": "id4",
          |      "apiStatus": "LIVE",
          |      "reviewedDate": "2024-10-10T09:56:23.385+0000",
          |      "title": "zebras",
          |      "description": "zebras api",
          |      "endpoints": [],
          |      "platform": "HIP",
          |      "hods": [],
          |      "publisherReference": "ref4",
          |      "openApiSpecification": "oas",
          |      "lastUpdated": "2024-10-10T09:56:23.385+0000"
          |    }
          |  ]
          |}
          |
          |""".stripMargin
      val parsedJson = Json.parse(json).as[IntegrationResponse]

      parsedJson.results.forall(_.openApiSpecification.isBlank) mustBe true

    }
  }

}
