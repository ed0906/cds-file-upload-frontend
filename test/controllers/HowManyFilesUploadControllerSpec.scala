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

import controllers.actions._
import forms.FileUploadCountProvider
import generators.Generators
import models.{BatchFileUpload, FileUploadCount, FileUploadResponse, MRN}
import models.requests.SignedInUser
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import pages.{HowManyFilesUploadPage, MrnEntryPage}
import play.api.data.Form
import play.api.libs.json.{JsNumber, JsString}
import play.api.test.Helpers._
import services.CustomsDeclarationsService
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.how_many_files_upload

import scala.concurrent.Future
import scala.util.Try

class HowManyFilesUploadControllerSpec extends ControllerSpecBase
  with MockitoSugar
  with PropertyChecks
  with Generators
  with FakeActions
  with BeforeAndAfterEach
  with OptionValues {

  def zip[A, B](ga: Gen[A], gb: Gen[B]): Gen[(A, B)] =
    ga.flatMap(a => gb.map(b => (a, b)))

  implicit val arbitraryMrnCacheMap: Arbitrary[CacheMap] =
    Arbitrary {
      zip(arbitraryCacheMap.arbitrary, arbitraryMrn.arbitrary).map {
        case (cacheMap, mrn) =>
          cacheMap.copy(data = cacheMap.data + (MrnEntryPage.toString -> JsString(mrn.value)))
      }
    }

  val form = new FileUploadCountProvider()()
  val service = mock[CustomsDeclarationsService]

  override def beforeEach = {
    super.beforeEach

    reset(service)

    when(service.batchFileUpload(any(), any(), any())(any()))
      .thenReturn(Future.successful(FileUploadResponse(List())))
  }

  def controller(dataRetrieval: DataRetrievalAction = getEmptyCacheMap) =
    new HowManyFilesUploadController(
      messagesApi,
      new FakeAuthAction(),
      new FakeEORIAction(),
      dataRetrieval,
      new MrnRequiredActionImpl,
      new FileUploadCountProvider,
      dataCacheConnector,
      service,
      appConfig)

  def viewAsString(form: Form[_] = form) = how_many_files_upload(form)(fakeRequest, messages, appConfig).toString

  "How Many Files Upload Page" must {
    "load correct page when user is logged in " in {

      forAll { cacheMap: CacheMap =>

        val result = controller(getCacheMap(cacheMap)).onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString(form)
      }
    }

    "display file count if it exist on the cache" in {

      forAll { (cacheMap: CacheMap, fileUploadCount: FileUploadCount) =>

        val updatedCacheMap = cacheMap.copy(data = cacheMap.data + (HowManyFilesUploadPage.toString -> JsNumber(fileUploadCount.value)))
        val result = controller(getCacheMap(updatedCacheMap)).onPageLoad(fakeRequest)

        contentAsString(result) mustBe viewAsString(form.fill(fileUploadCount))
      }
    }

    "load session expired page when data does not exist" when {

      "onPageLoad is called" in {

        val result = controller(getEmptyCacheMap).onPageLoad(fakeRequest)

        status(result) mustBe SEE_OTHER
      }

      "onSubmit is called" in {

        val result = controller(getEmptyCacheMap).onSubmit(fakeRequest)

        status(result) mustBe SEE_OTHER
      }
    }

    "return an ok when valid data is submitted" in {

      forAll { (cacheMap: CacheMap, fileUploadCount: FileUploadCount, response: FileUploadResponse) =>

        when(service.batchFileUpload(any(), any(), any())(any()))
          .thenReturn(Future.successful(response))

        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> fileUploadCount.value.toString)
        val result = controller(getCacheMap(cacheMap)).onSubmit(postRequest)
        val nextRef = response.files.map(_.reference).min

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UploadYourFilesController.onPageLoad(nextRef).url)
      }
    }

    "return a bad request when invalid data is submitted" in {

      forAll { (cacheMap: CacheMap, fileUploadCount: String) =>

        val fileUploadOpt =
          Try(fileUploadCount.toInt)
            .toOption
            .flatMap(FileUploadCount(_))

        whenever(fileUploadOpt.isEmpty) {

          val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> fileUploadCount)
          val boundForm = form.bind(Map("value" -> fileUploadCount))

          val result = controller(getCacheMap(cacheMap)).onSubmit(postRequest)

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe viewAsString(boundForm)
        }
      }
    }

    "save data in cache when valid" in {

      forAll { (cacheMap: CacheMap, fileUploadCount: FileUploadCount) =>

        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> fileUploadCount.value.toString)
        await(controller(getCacheMap(cacheMap)).onSubmit(postRequest))

        val captor: ArgumentCaptor[CacheMap] = ArgumentCaptor.forClass(classOf[CacheMap])
        verify(dataCacheConnector, atLeastOnce).save(captor.capture())
        captor.getValue.getEntry[FileUploadCount](HowManyFilesUploadPage) mustBe Some(fileUploadCount)
      }
    }

    "make a request to customs declarations" in {

      forAll { (cacheMap: CacheMap, fileUploadCount: FileUploadCount) =>

        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> fileUploadCount.value.toString)
        await(controller(getCacheMap(cacheMap)).onSubmit(postRequest))

        verify(service, times(1))
          .batchFileUpload(any(), eqTo(cacheMap.getEntry[MRN](MrnEntryPage).get), eqTo(fileUploadCount))(any())
      }
    }

    "save response in cache when valid" in {

      forAll { (cacheMap: CacheMap, fileUploadCount: FileUploadCount, response: FileUploadResponse) =>

        when(service.batchFileUpload(any(), any(), any())(any()))
          .thenReturn(Future.successful(response))

        val postRequest = fakeRequest.withFormUrlEncodedBody("value" -> fileUploadCount.value.toString)
        await(controller(getCacheMap(cacheMap)).onSubmit(postRequest))

        val mrn = cacheMap.getEntry[MRN](MrnEntryPage).value

        val captor: ArgumentCaptor[CacheMap] = ArgumentCaptor.forClass(classOf[CacheMap])
        verify(dataCacheConnector, atLeastOnce).save(captor.capture())
        captor.getValue.getEntry[BatchFileUpload](HowManyFilesUploadPage.Response) mustBe Some(BatchFileUpload(mrn, response.files))
      }
    }
  }
}
