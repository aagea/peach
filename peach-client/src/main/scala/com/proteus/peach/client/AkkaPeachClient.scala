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

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterClient
import akka.pattern.ask
import akka.util.Timeout
import com.proteus.peach.client.PeachAkkaClient.Log
import com.proteus.peach.client.config.PeachClientConfig
import com.proteus.peach.common.comm.PeachServerMessage.Get
import com.proteus.peach.common.comm.PeachServerMessage.GetResponse
import com.proteus.peach.common.comm.PeachServerMessage.Invalidate
import com.proteus.peach.common.comm.PeachServerMessage.InvalidateAll
import com.proteus.peach.common.comm.PeachServerMessage.InvalidateResponse
import com.proteus.peach.common.comm.PeachServerMessage.Put
import com.proteus.peach.common.comm.PeachServerMessage.PutResponse
import com.proteus.peach.common.comm.PeachServerMessage.Size
import com.proteus.peach.common.comm.PeachServerMessage.SizeResponse
import com.typesafe.config.ConfigFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.TimeoutException

/**
 * PeachClientCache companion object.
 */
object PeachAkkaClient {

  /**
   * Logger instance.
   */
  val Log: Logger = LoggerFactory.getLogger(classOf[PeachAkkaClient])

  /**
   * Create a PeachClientCache init the actor system.
   *
   * @param config Peach client config.
   * @return The cache object.
   */
  def apply(config: PeachClientConfig = PeachClientConfig.DefaultConfig): PeachClient = {
    val system = ActorSystem(config.clientName, ConfigFactory.load().getConfig(config.akkaConfig))

    val contactPoints = config.contactPoints.toArray[String](new Array[String](config.contactPoints.size()))
    val initialContacts = contactPoints.map(point => {
      system.actorSelection(s"akka.tcp://${config.serverName}@${point}${config.receptionistAddress}")
    }).toSet

    val client = system.actorOf(ClusterClient.props(initialContacts))

    new PeachAkkaClient(client, PeachClientConfig.DefaultConfig)
  }

}


/**
 * Proxy class to cluster client cache.
 *
 * @param clusterClient Cluster client actor.
 * @param config        Configuration properties.
 */
class PeachAkkaClient(clusterClient: ActorRef, config: PeachClientConfig) extends PeachClient {
  /**
   * Request timeout.
   */
  implicit val timeout: Timeout = Timeout(this.config.timeout.toSeconds, TimeUnit.SECONDS)

  /**
   * Put a element in the cache.
   *
   * @param key   Searched key.
   * @param value Value data.
   * @return A put response.
   * @throws InterruptedException if the current thread is interrupted while waiting
   * @throws TimeoutException     if after waiting for the specified time `awaitable` is still not ready
   */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  override def put(key: String, value: String): Unit = {
    if (Option(key).isEmpty) {
      throw new IllegalArgumentException("The key must not be NULL.")
    }
    val response = clusterClient ? ClusterClient.Send(this.config.receptorAddress, Put(key, value),
      localAffinity = true)
    Await.result(response, this.config.timeout) match {
      case PutResponse() => {
        Log.debug(s"Correct insertion")
      }
      case _ => {
        throw new UnsupportedOperationException("Receptor has sent an unexpected message.")
      }
    }

  }

  /**
   * Recover a element.
   *
   * @param key Searched key
   * @return The value if exist.
   * @throws InterruptedException if the current thread is interrupted while waiting
   * @throws TimeoutException     if after waiting for the specified time `awaitable` is still not ready
   */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  override def get(key: String): Option[String] = {
    val response = clusterClient ? ClusterClient.Send(this.config.receptorAddress, Get(key),
      localAffinity = true)
    Await.result(response, this.config.timeout) match {
      case response: GetResponse => {
        response.value
      }
      case _ => {
        throw new UnsupportedOperationException("Receptor has sent an unexpected message.")
      }
    }
  }

  /**
   * Get a element using an async approach.
   *
   * @param key Searched key.
   * @return A future with the value.
   * @throws InterruptedException if the current thread is interrupted while waiting
   * @throws TimeoutException     if after waiting for the specified time `awaitable` is still not ready
   */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  override def getAsync(key: String): Future[Option[String]] = {
    val response = clusterClient ? ClusterClient.Send(this.config.receptorAddress, Get(key),
      localAffinity = true)
    response.map {
      case response: GetResponse => {
        response.value
      }
      case _ => {
        throw new UnsupportedOperationException("Receptor has sent an unexpected message.")
      }
    }
  }

  /**
   * Discards any cached value for key key.
   *
   * @param key Searched key.
   * @throws InterruptedException if the current thread is interrupted while waiting
   * @throws TimeoutException     if after waiting for the specified time `awaitable` is still not ready
   */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  override def invalidate(key: String): Unit = {
    val response = clusterClient ? ClusterClient.Send(this.config.receptorAddress, Invalidate(key),
      localAffinity = true)
    Await.result(response, this.config.timeout) match {
      case InvalidateResponse() => {
        Log.debug(s"Correct invalidation.")
      }
      case _ => {
        throw new UnsupportedOperationException("Receptor has sent an unexpected message.")
      }
    }
  }

  /**
   * Discards all entries in the cache.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting
   * @throws TimeoutException     if after waiting for the specified time `awaitable` is still not ready
   */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  override def invalidateAll(): Unit = {
    val response = clusterClient ? ClusterClient.Send(this.config.receptorAddress, InvalidateAll(),
      localAffinity = true)
    Await.result(response, this.config.timeout) match {
      case InvalidateResponse() => {
        Log.debug(s"Correct invalidation.")
      }
      case _ => {
        throw new UnsupportedOperationException("Receptor has sent an unexpected message.")
      }
    }
  }

  /**
   * Returns the approximate number of entries in this cache.
   *
   * @return The approximate number of entries.
   * @throws InterruptedException if the current thread is interrupted while waiting
   * @throws TimeoutException     if after waiting for the specified time `awaitable` is still not ready
   */
  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  override def size(): Long = {
    val response = clusterClient ? ClusterClient.Send(this.config.receptorAddress, Size(),
      localAffinity = true)
    Await.result(response, this.config.timeout) match {
      case response: SizeResponse => {
        response.value
      }
      case _ => {
        throw new UnsupportedOperationException("Receptor has sent an unexpected message.")
      }
    }
  }
}

