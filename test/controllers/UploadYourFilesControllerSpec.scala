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

import controllers.actions.{BatchFileUploadRequiredActionImpl, DataRetrievalAction, FakeActions}
import generators.Generators
import models._
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import pages.HowManyFilesUploadPage
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.upload_your_files


class UploadYourFilesControllerSpec extends ControllerSpecBase
  with ScalaFutures
  with MockitoSugar
  with PropertyChecks
  with Generators
  with FakeActions
  with OptionValues {

  val batchGen: Gen[(File, BatchFileUpload)] =
    for {
      mrn   <- arbitrary[MRN]
      count <- Gen.choose(1, 10)
      files <- Gen.listOfN(count, waitingFileGen)
      index <- Gen.choose(0, files.length - 1)
      file      = files(index)
    } yield (file, BatchFileUpload(mrn, files))

  def controller(getData: DataRetrievalAction) =
    new UploadYourFilesController(
      messagesApi,
      new FakeAuthAction(),
      new FakeEORIAction(),
      getData,
      new BatchFileUploadRequiredActionImpl(),
      dataCacheConnector,
      appConfig)

  def viewAsString(uploadRequest: UploadRequest, callbackUrl: String, refPosition: Position): String =
    upload_your_files(uploadRequest, callbackUrl, refPosition)(fakeRequest, messages, appConfig).toString

  private def combine(response: BatchFileUpload, cache: CacheMap): CacheMap =
    cache.copy(data = cache.data + (HowManyFilesUploadPage.Response.toString -> Json.toJson(response)))

  private def nextRef(ref: String, refs: List[String]): String = {
    val index = refs.sorted.indexOf(ref)
    refs.sorted.drop(index + 1).headOption.getOrElse("receipt")
  }

  private def uploadRequest(file: File): Option[UploadRequest] =
    file.state match {
      case Waiting(request) => Some(request)
      case _                => None
    }

  ".onPageLoad" should {

    "load the view" when {

      def nextPosition(ref: String, refs: List[String]): Position = {
        refs.indexOf(ref) match {
          case 0                           => First(refs.size)
          case x if x == (refs.length - 1) => Last(refs.size)
          case x                           => Middle(x + 1, refs.size)
        }
      }

      "request file exists in response" in {

        forAll(batchGen, arbitrary[CacheMap]) {
          case ((file, batch), cacheMap) =>

            val callback =
              routes.UploadYourFilesController.onSuccess(file.reference).absoluteURL()(fakeRequest)

            val refPosition: Position =
              nextPosition(file.reference, batch.files.map(_.reference))

            val updatedCache = combine(batch, cacheMap)
            val result = controller(getCacheMap(updatedCache)).onPageLoad(file.reference)(fakeRequest)

            status(result) mustBe OK
            contentAsString(result) mustBe viewAsString(uploadRequest(file).value, callback, refPosition)
        }
      }
    }

    "redirect to the next page" when {

      "file has already been uploaded" in {

        val fileUploadedGen = batchGen.map {
          case (file, batch) =>
            val uploadedFile = file.copy(state = Uploaded)
            val updatedFiles = uploadedFile :: batch.files.filterNot(_ == file)

            (uploadedFile, BatchFileUpload(batch.mrn, updatedFiles))
        }

        forAll(fileUploadedGen, arbitrary[CacheMap]) {
          case ((file, batch), cache) =>

            val reference    = nextRef(file.reference, batch.files.map(_.reference))
            val nextPage     = routes.UploadYourFilesController.onPageLoad(reference)
            val updatedCache = combine(batch, cache)

            val result = controller(getCacheMap(updatedCache)).onPageLoad(file.reference)(fakeRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(nextPage.url)
        }
      }
    }

    "redirect to session expired page" when {

      "no responses are in the cache" in {

        forAll { ref: String =>

          val result = controller(getEmptyCacheMap).onPageLoad(ref)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
        }
      }

      "file reference is not in response" in {

        forAll { (ref: String, batch: BatchFileUpload, cache: CacheMap) =>

          whenever(!batch.files.exists(_.reference == ref)) {

            val updatedCache = combine(batch, cache)
            val result = controller(getCacheMap(updatedCache)).onPageLoad(ref)(fakeRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
          }
        }
      }
    }
  }

  ".onSuccess" should {

    "update file status to Uploaded" in {

      forAll(batchGen, arbitrary[CacheMap]) {
        case ((file, batch), cache) =>

          val updatedCache = combine(batch, cache)
          val result = controller(getCacheMap(updatedCache)).onSuccess(file.reference)(fakeRequest)

          whenReady(result) { _ =>

            val captor: ArgumentCaptor[CacheMap] = ArgumentCaptor.forClass(classOf[CacheMap])
            verify(dataCacheConnector, atLeastOnce).save(captor.capture())

            val updatedBatch = captor.getValue.getEntry[BatchFileUpload](HowManyFilesUploadPage.Response)

            updatedBatch must not be Some(batch)
            updatedBatch
              .flatMap(_.files.find(_.reference == file.reference))
              .map(_.state) mustBe Some(Uploaded)
          }
      }
    }

    "redirect user to the next upload page" in {

      forAll(batchGen, arbitrary[CacheMap]) {
        case ((file, batch), cache: CacheMap) =>

          val updatedCache = combine(batch, cache)
          val result = controller(getCacheMap(updatedCache)).onSuccess(file.reference)(fakeRequest)
          val next = nextRef(file.reference, batch.files.map(_.reference))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UploadYourFilesController.onPageLoad(next).url)
      }
    }

    "redirect to session expired page" when {

      "no responses are in the cache" in {

        forAll { ref: String =>

          val result = controller(getEmptyCacheMap).onSuccess(ref)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
        }
      }

      "file reference is not in response" in {

        forAll { (ref: String, batch: BatchFileUpload, cache: CacheMap) =>

          whenever(!batch.files.exists(_.reference == ref)) {

            val updatedCache = combine(batch, cache)
            val result = controller(getCacheMap(updatedCache)).onSuccess(ref)(fakeRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
          }
        }
      }
    }
  }
}
