/*
 * Copyright (C) 2017 The Proteus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.proteus.peach.client

import java.util.{HashMap => JHashMap}
import java.util.{Map => JMap}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MockupPeachClient extends PeachClient {

  /**
   * Internal in memory cache.
   */

  val cache: JMap[String, String] = new JHashMap[String, String]()

  /**
   * Put a element in the cache.
   *
   * @param key   Searched key.
   * @param value Value data.
   * @return A put response.
   */
  override def put(key: String, value: String): Unit = {
    if (Option(key).isEmpty) {
      throw new IllegalArgumentException("The key must not be NULL.")
    }
    this.cache.put(key, value)
  }

  /**
   * Recover a element.
   *
   * @param key Searched key
   * @return The value if exist.
   */
  override def get(key: String): Option[String] = {
    Option(this.cache.get(key))
  }

  /**
   * Get a element using an async approach.
   *
   * @param key Searched key.
   * @return A future with the value.
   */
  override def getAsync(key: String): Future[Option[String]] = {
    Future(Option(this.cache.get(key)))
  }

  /**
   * Discards any cached value for key key.
   *
   * @param key Searched key.
   */
  override def invalidate(key: String): Unit = {
    this.cache.remove(key)
  }

  /**
   * Discards all entries in the cache.
   */
  override def invalidateAll(): Unit = {
    this.cache.clear()
  }

  /**
   * Returns the approximate number of entries in this cache.
   *
   * @return The approximate number of entries.
   */
  override def size(): Long = {
    this.cache.size()
  }
}
