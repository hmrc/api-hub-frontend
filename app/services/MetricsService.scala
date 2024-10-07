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
import com.google.inject.{Inject, Singleton}

@Singleton
class MetricsService @Inject()(metricRegistry: MetricRegistry) {

  import MetricsService.*

  private lazy val strideMissingEmailMetric: Counter = metricRegistry.counter(MetricsKeys.Authentication.strideMissingEmail)

  def strideMissingEmail(): Unit = {
    strideMissingEmailMetric.inc()
  }

}

private object MetricsService {

  private object MetricsKeys {

    object Authentication {

      val strideMissingEmail: String = "authentication-stride-missing-email"

    }

  }

}
