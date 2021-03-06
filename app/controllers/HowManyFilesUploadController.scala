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

import config.AppConfig
import connectors.DataCacheConnector
import controllers.actions.{AuthAction, DataRetrievalAction, EORIAction, MrnRequiredAction}
import forms.FileUploadCountProvider
import javax.inject.{Inject, Singleton}
import pages.HowManyFilesUploadPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.CustomsDeclarationsService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.how_many_files_upload

import scala.concurrent.Future

@Singleton
class HowManyFilesUploadController @Inject()(
                                              val messagesApi: MessagesApi,
                                              authenticate: AuthAction,
                                              requireEori: EORIAction,
                                              getData: DataRetrievalAction,
                                              requireMrn: MrnRequiredAction,
                                              formProvider: FileUploadCountProvider,
                                              dataCacheConnector: DataCacheConnector,
                                              customsDeclarationsService: CustomsDeclarationsService,
                                              implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  val form = formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireMrn) { implicit req =>

      val populatedForm =
        req.userAnswers
          .get(HowManyFilesUploadPage)
          .map(form.fill).getOrElse(form)

      Ok(how_many_files_upload(populatedForm))
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen requireEori andThen getData andThen requireMrn).async { implicit req =>

      form.bindFromRequest().fold(
        errorForm =>
          Future.successful(BadRequest(how_many_files_upload(errorForm))),

        value => {
          customsDeclarationsService
            .batchFileUpload(req.request.eori, req.mrn, value)
            .flatMap { response =>

              val answers =
                req.userAnswers
                  .set(HowManyFilesUploadPage, value)
                  .set(HowManyFilesUploadPage.Response, response)

              dataCacheConnector.save(answers.cacheMap).map { _ =>

                response.files.map(_.reference).headOption match {
                  case Some(nextRef) => Redirect(routes.UploadYourFilesController.onPageLoad(nextRef))
                  case None          => Redirect(routes.SessionExpiredController.onPageLoad())
                }
              }
            }
        }
      )
    }
}
