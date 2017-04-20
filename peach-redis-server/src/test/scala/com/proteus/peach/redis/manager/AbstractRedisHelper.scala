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

package com.proteus.peach.redis.manager

import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.BeforeClass

/**
 * Abstract class to run Redis integration tests. Redis must be launched before starting the
 * tests.
 */
object AbstractRedisHelper{

  /**
   * Redis helper.
   */
  val redisHelper : RedisHelper = this.initHelper()

  /**
   * Get the redis target host.
   * @return The host specified in REDIS_HOST or the default host.
   */
  def getRedisHost() : String = {
    sys.env.getOrElse(RedisHelper.HostEnvironmentVariable, RedisHelper.DefaultHost)
  }

  /**
   * Get the redis target port.
   * @return The port specified in REDIS_PORT or the default port.
   */
  def getRedisPort() : Int = {
    sys.env.getOrElse(RedisHelper.PortEnvironmentVariable,
      s"${RedisHelper.DefaultPort}").toInt
  }

  /**
   * Init the connection with redis taking into account the environment variables.
   */
  private def initHelper() : RedisHelper = {
    new RedisHelper(AbstractRedisHelper.getRedisHost(), AbstractRedisHelper.getRedisPort())
  }

  @BeforeClass
  def beforeRedisIT(): Unit = {
    this.redisHelper.connect()
    assertTrue("Redis is not available", this.redisHelper.isAlive())
    this.redisHelper.flushAll()
  }

  @AfterClass
  def afterRedisIT(): Unit = {
    this.redisHelper.close()
  }
}

abstract class AbstractRedisHelper {

  /**
   * Assert that a key in a hashmap matches the expected value.
   * @param map The name of the map.
   * @param key The key in the map.
   * @param value The expected value.
   */
  def assertKeyEquals(map: String, key: String, value: String) : Unit = {
    AbstractRedisHelper.redisHelper.assertKeyEquals(map, key, value)
  }

  /**
   * Remove a list of keys from redis.
   * @param keys The keys.
   */
  def removeKeys(keys: String*) : Unit = {
    AbstractRedisHelper.redisHelper.removeKeys(keys)
  }
}
