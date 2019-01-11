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

import generators.Generators
import models.{BatchListFields, BatchListFiles}
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.{batch_list, upload_your_files_receipt}

class BatchListSpec extends ViewSpecBase with ViewBehaviours with PropertyChecks with Generators {

  def view(batchListFiles: List[BatchListFiles]): Html =
    batch_list(batchListFiles)(fakeRequest, messages, appConfig)

  val blf1 = BatchListFiles("12313123123132",
    List(
      BatchListFields("invoice.png", "receiptXXXXXX", "Failed-reason unknown")
    )
  )

  val blf2 = BatchListFiles("12313123123132",
    List(
      BatchListFields("invoice.png", "receiptXXXXXX", "Failed-reason unknown")
    )
  )

  val view: () => Html = () => batch_list(List(blf1, blf2))(fakeRequest, messages, appConfig)

  val messageKeyPrefix = "batchListPage"

  "Batch List Page" must {
    behave like normalPage(view, messageKeyPrefix)

    "display all mrns" in {
      forAll(batchListFilesGen) { batchListFile =>

        val doc = asDocument(view(List(batchListFile)))

        assertContainsText(doc, batchListFile.mrn)
      }
    }

    "display all file names" in {
      forAll(batchListFilesGen) { batchListFile =>

        val doc = asDocument(view(List(batchListFile)))

        batchListFile.batchListFields.foreach(field => assertContainsText(doc, field.fileName))
      }
    }

    "display all receipts" in {
      forAll(batchListFilesGen) { batchListFile =>

        val doc = asDocument(view(List(batchListFile)))

        batchListFile.batchListFields.foreach(field => assertContainsText(doc, field.receipt))
      }
    }

    "display all status" in {
      forAll(batchListFilesGen) { batchListFile =>

        val doc = asDocument(view(List(batchListFile)))

        batchListFile.batchListFields.foreach(field => assertContainsText(doc, field.status))
      }
    }
  }
}
