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

package connectors

import com.google.inject.Inject
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.CacheMap
import repositories.CacheMapRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoCacheConnector @Inject()(val repository: CacheMapRepository) extends DataCacheConnector {

  def save[A](cacheMap: CacheMap): Future[CacheMap] = {
    repository.put(cacheMap).map{ _ => cacheMap}
  }

  def fetch(cacheId: String): Future[Option[CacheMap]] =
    repository.get(cacheId)

  def getEntry[A](cacheId: String, key: String)(implicit fmt: Format[A]): Future[Option[A]] = {
    fetch(cacheId).map { optionalCacheMap =>
      optionalCacheMap.flatMap { cacheMap => cacheMap.getEntry(key)}
    }
  }
}

trait DataCacheConnector {
  def save[A](cacheMap: CacheMap): Future[CacheMap]

  def fetch(cacheId: String): Future[Option[CacheMap]]

  def getEntry[A](cacheId: String, key: String)(implicit fmt: Format[A]): Future[Option[A]]
}
