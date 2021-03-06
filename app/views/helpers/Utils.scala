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

package views.helpers

import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html

object Utils {

  def errorPrefix(form: Form[_])(implicit messages: Messages): String = {
    if (form.hasErrors || form.hasGlobalErrors) messages("error.browser.title.prefix") else ""
  }

  def multiline(values: String*): Html = {
    Html(values.toList.mkString("<br />"))
  }

  def bold(value: Html): Html = bold(value.body)

  def bold(value: String): Html = {
    Html(s"<strong class='bold'>$value</strong>")
  }

  def email(address: String): Html = {
    Html(s"""<a href="email:$address">$address</a>""")
  }

  def paragraph(content: Html): Html = paragraph(content.body)

  def paragraph(content: String): Html = {
    Html(s"<p>$content</p>")
  }
}