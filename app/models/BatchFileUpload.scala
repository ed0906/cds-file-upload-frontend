/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json, OFormat, Reads}
import play.json.extra.Variants
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

sealed abstract case class BatchFileUpload(mrn: MRN, files: List[File])

object BatchFileUpload {

  def apply(mrn: MRN, files: List[File]): BatchFileUpload =
    new BatchFileUpload(mrn, files.sortBy(_.reference)) {}


  implicit val reads: Reads[BatchFileUpload] =
    ((__ \ "mrn").read[MRN] and
      (__ \ "files").read[List[File]]
    )(BatchFileUpload.apply _)

  implicit val writes: Writes[BatchFileUpload] = Writes {
    case BatchFileUpload(mrn, files) =>
      JsObject(Map(("mrn" -> Json.toJson(mrn)), ("files" -> Json.toJson(files))))
  }

}
