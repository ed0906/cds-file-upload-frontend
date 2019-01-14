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

import play.api.libs.json._
import play.json.extra.Variants

import scala.xml.Elem

case class UploadRequest(href: String, fields: Map[String, String])

object UploadRequest {

  implicit val format = Json.format[UploadRequest]
}

sealed trait FileState
final case class Waiting(uploadRequest: UploadRequest) extends FileState
final case object Uploaded extends FileState
final case object Successful extends FileState
final case object Failed extends FileState
final case object VirusDetected extends FileState
final case object UnacceptableMimeType extends FileState

object Waiting {

  implicit val format = Json.format[Waiting]
}

object FileState {

  implicit val format = Variants.format[FileState]
}

sealed case class File(reference: String, state: FileState)

object File {

  implicit val format = Json.format[File]
}

sealed case class FileUploadResponse(files: List[File])

object FileUploadResponse {

  implicit val format = Variants.format[FileUploadResponse]

  def fromXml(xml: Elem): FileUploadResponse = {
    val files: List[File] = (xml \ "Files" \ "_").theSeq.collect {
      case file =>
        val reference = (file \ "reference").text.trim
        val href = (file \ "uploadRequest" \ "href").text.trim
        val fields: Map[String, String] = (file \ "uploadRequest" \ "fields" \ "_").theSeq.collect {
          case field =>
            field.label -> field.text.trim
        }.toMap
        File(reference, Waiting(UploadRequest(href, fields)))
    }.toList

    FileUploadResponse(files)
  }
}
