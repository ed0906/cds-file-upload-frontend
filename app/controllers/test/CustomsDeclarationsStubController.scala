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

package controllers.test

import java.util.UUID

import javax.inject.{Inject, Singleton}
import models.Field._
import models.{File, FileUploadResponse, UploadRequest, Waiting}
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.ContentTypes
import play.api.libs.Files
import play.api.mvc.{Action, MultipartFormData}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.xml._

@Singleton
class CustomsDeclarationsStubController @Inject()() extends FrontendController {

  val s3Form = Form(mapping(
    "success_action_redirect" -> text
  )(S3Form.apply)(S3Form.unapply))

  // for now, we will just return some random
  def handleBatchFileUploadRequest: Action[NodeSeq] = Action(parse.xml) { implicit req =>
    val fileGroupSize = (scala.xml.XML.loadString(req.body.mkString) \ "FileGroupSize").text.toInt
    val resp = FileUploadResponse((1 to fileGroupSize).map { i =>
      File(reference = UUID.randomUUID().toString, Waiting(UploadRequest(
        href = "/cds-file-upload-service/test-only/s3-bucket",
        fields = Map(
          Algorithm.toString   -> "AWS4-HMAC-SHA256",
          Signature.toString   -> "xxxx",
          Key.toString         -> "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
          ACL.toString         -> "private",
          Credentials.toString -> "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
          Policy.toString      -> "xxxxxxxx=="
        )
      )))
    }.toList)
    Ok(XmlHelper.toXml(resp)).as(ContentTypes.XML)
  }

  def handleS3FileUploadRequest: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit req =>
    s3Form.bindFromRequest().fold(
      errors => NoContent,
      success => Redirect(success.success_action_redirect)
    )
  }
}

case class S3Form(success_action_redirect: String)

object XmlHelper {

  def toXml(field: (String, String)): Elem =
      <a/>.copy(label = field._1, child = Seq(Text(field._2)))

  def toXml(uploadRequest: UploadRequest): Elem =
    <UploadRequest>
      <Href>{uploadRequest.href}</Href>
      <Fields>
        {uploadRequest.fields.map(toXml)}
      </Fields>
    </UploadRequest>

  def toXml(file: File): Elem =
    <File>
      <Reference>{file.reference}</Reference>{file.state match {
        case Waiting(request) => toXml(request)
        case _ => NodeSeq.Empty
      }}
    </File>

  def toXml(response: FileUploadResponse): Elem = {
    <FileUploadResponse xmlns="hmrc:fileupload">
      <Files>{response.files.map(toXml)}</Files>
    </FileUploadResponse>
  }

}
