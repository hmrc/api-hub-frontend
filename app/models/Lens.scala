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

case class Lens[A, B](
  get: A => B,
  set: (A, B) => A
)

object Lens {

  def compose[A, B, C](
    outer: Lens[A, B],
    inner: Lens[B, C]
  ): Lens[A, C] =
    Lens[A, C](
      get = outer.get andThen inner.get,
      set = (b, c) => outer.set(b, inner.set(outer.get(b), c))
    )

}
