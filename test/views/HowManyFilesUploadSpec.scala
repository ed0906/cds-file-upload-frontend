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

package views

import forms.FileUploadCountProvider
import models.FileUploadCount
import org.scalatest.prop.PropertyChecks
import play.api.data.Form
import play.twirl.api.{Html, HtmlFormat}
import views.behaviours.{IntViewBehaviours, StringViewBehaviours}
import views.html.how_many_files_upload

class HowManyFilesUploadSpec extends ViewSpecBase with IntViewBehaviours[FileUploadCount] with PropertyChecks {

  val form = new FileUploadCountProvider()()

  val view: () => Html = () => how_many_files_upload(form)(fakeRequest, messages, appConfig)

  val messagePrefix = "howManyFilesUpload"

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => how_many_files_upload(form)(fakeRequest, messages, appConfig)

  "How Many Files Upload Page" must {
    behave like normalPage(view, messagePrefix)
    behave like intPage(
        createViewUsingForm,
        (form, i) => form.bind(Map("value" -> i.toString)),
        "value",
        messagePrefix)
  }
}
