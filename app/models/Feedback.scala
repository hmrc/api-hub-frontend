/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import models.Feedback.FeedbackType

case class Feedback(
  `type`: FeedbackType,
  otherType: Option[String],
  rate: Int,
  comments: String,
  allowContact: Boolean,
  email: Option[String]
)

object Feedback:
  enum FeedbackType:
    case General extends FeedbackType
    case FeatureSuggestion extends FeedbackType
    case Other extends FeedbackType
