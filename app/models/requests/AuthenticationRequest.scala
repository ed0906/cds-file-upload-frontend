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

package models.requests

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.retrieve._

case class SignedInUser(
  credentials: Credentials,
  name: Name,
  email: Option[String],
  affinityGroup: Option[AffinityGroup],
  internalId: String,
  enrolments: Enrolments)

object SignedInUser {

  val cdsEnrolmentName: String = "HMRC-CUS-ORG"

  val eoriIdentifierKey: String = "EORINumber"

  val authorisationPredicate: Predicate = Enrolment(cdsEnrolmentName)

}

case class AuthenticatedRequest[A](request: Request[A], user: SignedInUser) extends WrappedRequest[A](request)

case class EORIRequest[A](request: AuthenticatedRequest[A], eori: String) extends WrappedRequest[A](request) {

  val user = request.user
}
