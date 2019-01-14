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
import controllers.actions.{AuthAction, DataRetrievalAction, EORIAction, BatchFileUploadRequiredAction}
import forms.MRNFormProvider
import javax.inject.Inject
import models.{BatchListFields, BatchListFiles}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.{batch_list, upload_your_files}

class BatchListFilesController @Inject()(
                                          val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          requireEori: EORIAction,
                                          getData: DataRetrievalAction,
                                          requireResponse: BatchFileUploadRequiredAction,
                                          dataCacheConnector: DataCacheConnector,
                                          implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {


  def onPageLoad: Action[AnyContent] = (authenticate andThen requireEori andThen getData andThen requireResponse)
  { implicit req =>

    val x =
      req.batchFileUpload.files.map(file => BatchListFields("", file.reference, file.state.toString))

    val blf1 = BatchListFiles("12313123123132",
      List(BatchListFields("invoice.png", "receiptXXXXXX", "Failed-reason unknown"),
           BatchListFields("invoice.png", "receiptXXXXXX", "Failed-reason unknown"),
           BatchListFields("invoice.png", "receiptXXXXXX", "Failed-reason unknown")))

    val blf2 = BatchListFiles("12313123123132",
      List(BatchListFields("invoice.png", "receiptXXXXXX", "Failed-reason unknown"),
           BatchListFields("invoice.png", "receiptXXXXXX", "Failed-reason unknown"),
           BatchListFields("invoice.png", "receiptXXXXXX", "Failed-reason unknown")))

    Ok(batch_list(List(blf1, blf2)))
  }
}
