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

package controllers

import com.google.inject.Singleton
import config.AppConfig
import connectors.DataCacheConnector
import controllers.actions._
import javax.inject.Inject
import models._
import pages.HowManyFilesUploadPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.upload_your_files

import scala.concurrent.Future

@Singleton
class UploadYourFilesController @Inject()(
                                           val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           requireEori: EORIAction,
                                           getData: DataRetrievalAction,
                                           requireResponse: BatchFileUploadRequiredAction,
                                           dataCacheConnector: DataCacheConnector,
                                           implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  def onPageLoad(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse) { implicit req =>

      val references  = req.batchFileUpload.files.map(_.reference)
      val callback    = routes.UploadYourFilesController.onSuccess(ref).absoluteURL()
      val refPosition = getPosition(ref, references)

      req.batchFileUpload.files.find(_.reference == ref) match {
        case Some(file) =>
          file.state match {
            case Waiting => Ok(upload_your_files(file.uploadRequest, callback, refPosition))
            case _       => Redirect(nextPage(file.reference, req.batchFileUpload.files))
          }

        case None => Redirect(routes.SessionExpiredController.onPageLoad())
      }
  }

  def onSuccess(ref: String): Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireResponse).async { implicit req =>

      val files  = req.batchFileUpload.files

      files.find(_.reference == ref) match {
        case Some(file) =>
          val updatedFiles = file.copy(state = Uploaded) :: files.filterNot(_.reference == ref)
          val updatedBatch = req.batchFileUpload.copy(files = updatedFiles)
          val answers = req.userAnswers.set(HowManyFilesUploadPage.Response, updatedBatch)

          dataCacheConnector.save(answers.cacheMap).map { _ =>
            Redirect(nextPage(ref, files))
          }

        case None => Future.successful(Redirect(routes.SessionExpiredController.onPageLoad()))
      }
    }

  def nextPage(ref: String, refs: List[File])(implicit request: Request[_]): Call =
    refs
      .partition(_.reference <= ref)._2
      .find(_.state == Waiting)
      .map(file => routes.UploadYourFilesController.onPageLoad(file.reference))
      .getOrElse(routes.UploadYourFilesReceiptController.onPageLoad())

  def getPosition(ref: String, refs: List[String]): Position =
    if (refs.headOption.contains(ref)) First(refs.size)
    else if (refs.lastOption.contains(ref)) Last(refs.size)
    else Middle(refs.indexOf(ref) + 1, refs.size)
}

sealed trait Position

case class First(total: Int)              extends Position
case class Middle(index: Int, total: Int) extends Position
case class Last(total: Int)               extends Position
