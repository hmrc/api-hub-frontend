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

package services

import com.codahale.metrics.{Counter, MetricRegistry}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.{verify, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar

class MetricsServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  "MetricsService.strideMissingEmail" - {
    "must increment the correct metric" in {
      val fixture = buildFixture()

      val counter = mock[Counter]

      when(fixture.metricRegistry.counter(eqTo(MetricsService.MetricsKeys.Authentication.strideMissingEmail)))
        .thenReturn(counter)

      fixture.metricsService.strideMissingEmail()

      verify(counter).inc()
    }
  }

  private case class Fixture(metricRegistry: MetricRegistry, metricsService: MetricsService)

  private def buildFixture(): Fixture = {
    val metricRegistry = mock[MetricRegistry]
    val metricsService = new MetricsService(metricRegistry)

    Fixture(metricRegistry, metricsService)
  }

}
