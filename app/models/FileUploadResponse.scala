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

case class File(reference: String, state: FileState)

object File {

  implicit val format = Json.format[File]
}

sealed abstract case class FileUploadResponse(files: List[File])

object FileUploadResponse {

  def apply(files: List[File]): FileUploadResponse =
    new FileUploadResponse(files.sortBy(_.reference)) {}

  implicit val format = Json.format[FileUploadResponse]

  def fromXml(xml: Elem): FileUploadResponse = {
    val files: List[File] = (xml \ "Files" \ "_").theSeq.collect {
      case file =>
        val reference = (file \ "Reference").text.trim
        val href = (file \ "UploadRequest" \ "Href").text.trim
        val fields: Map[String, String] =
          (file \ "UploadRequest" \ "Fields" \ "_")
            .theSeq
            .map(field => field.label -> field.text.trim)
            .toMap

        File(reference, Waiting(UploadRequest(href, fields)))
    }.toList

    FileUploadResponse(files)
  }
}

abstract class Field(value: String) {
  override def toString: String = value
}

object Field {

  final case object ContentType extends Field("Content-Type")
  final case object ACL         extends Field("acl")
  final case object Key         extends Field("key")
  final case object Policy      extends Field("policy")
  final case object Algorithm   extends Field("x-amz-algorithm")
  final case object Credentials extends Field("x-amz-credential")
  final case object Date        extends Field("x-amz-date")
  final case object Callback    extends Field("x-amz-meta-callback-url")
  final case object Signature   extends Field("x-amz-signature")

  val values: Set[Field] = Set(
    ContentType, ACL, Key, Policy, Algorithm, Credentials, Date, Signature, Callback
  )
}
