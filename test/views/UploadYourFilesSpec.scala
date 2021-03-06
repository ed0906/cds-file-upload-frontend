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

import controllers.{First, Last, Middle, Position}
import generators.Generators
import org.scalatest.prop.PropertyChecks
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.upload_your_files

class UploadYourFilesSpec extends ViewSpecBase with ViewBehaviours with PropertyChecks with Generators {

  def view(pos: Position): Html =
    upload_your_files("", pos)(fakeRequest, messages, appConfig)

  val view: () => Html = () => view(First(3))

  val messagePrefix = "fileUploadPage"

  val messageKeys = List(
                          "p.fileNeedsToBe",
                          "listItem1",
                          "listItem2",
                          "listItem3",
                          "listItem4",
                          "listItem5",
                          "listItem6"
                        )

  "Upload your files page" must {

    behave like pageWithoutHeading(view, messagePrefix, messageKeys: _*)

    "show title" when {

      "first file upload is shown" in {

        forAll { total: Int =>

          assertEqualsMessage(asDocument(view(First(total))), "title", s"$messagePrefix.title.first")
        }
      }

      "a middle file upload is shown" in {

        forAll { (index: Int, total: Int) =>

          assertEqualsMessage(asDocument(view(Middle(index, total))), "title", s"$messagePrefix.title.middle")
        }
      }

      "the last file upload is shown" in {

        forAll { total: Int =>

          assertEqualsMessage(asDocument(view(Last(total))), "title", s"$messagePrefix.title.last")
        }
      }
    }

    "show heading" when {

      "first file upload is shown" in {

        forAll { total: Int =>

          assertPageTitleEqualsMessage(asDocument(view(First(total))), s"$messagePrefix.heading.first")
        }
      }

      "a middle file upload is shown" in {

        forAll { (index: Int, total: Int) =>

          assertPageTitleEqualsMessage(asDocument(view(Middle(index, total))), s"$messagePrefix.heading.middle")
        }
      }

      "the last file upload is shown" in {

        forAll { total: Int =>

          assertPageTitleEqualsMessage(asDocument(view(Last(total))), s"$messagePrefix.heading.last")
        }
      }
    }

    "show file upload counter" when {

      "a file middle is requested" in {

        forAll { (index: Int, total: Int) =>

          val doc = asDocument(view(Middle(index, total)))
          assertContainsMessage(doc, s"$messagePrefix.filesUploaded", index - 1, total)
        }
      }

      "the last file is requested" in {

        forAll { total: Int =>

          val doc = asDocument(view(Last(total)))
          assertContainsMessage(doc, s"$messagePrefix.filesUploaded", total - 1, total)
        }
      }
    }
  }

}
