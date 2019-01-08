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

package controllers.actions

import com.google.inject.Inject
import controllers.routes
import models.requests.{DataRequest, FieldRequest}
import pages.QuestionPage
import play.api.libs.json.Reads
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future

class FieldRequiredActionProviderImpl @Inject() extends FieldRequiredActionProvider {

  def apply[Field: Reads](page: QuestionPage[Field]): ActionRefiner[DataRequest, ({type λ[+A] = FieldRequest[A, Field]})#λ] =
    new ActionRefiner[DataRequest, ({type λ[+A] = FieldRequest[A, Field]})#λ] {

      override protected def refine[A](request: DataRequest[A]): Future[Either[Result, FieldRequest[A, Field]]] = {
        implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

        Future.successful(
          request.userAnswers
            .get(page)
            .map(field => FieldRequest(request.request, request.userAnswers, field))
            .toRight(Redirect(routes.SessionExpiredController.onPageLoad())))
      }
    }
}

trait FieldRequiredActionProvider {

  def apply[Field: Reads](page: QuestionPage[Field]): ActionRefiner[DataRequest, ({type λ[+A] = FieldRequest[A, Field]})#λ]
}
