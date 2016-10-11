package com.wehkamp.basket.utils

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

trait Config {
  private val config = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")

  val httpInterface = httpConfig.getString("interface")
  val httpPort = httpConfig.getInt("port")

  val defaultAskTimeout = 10.seconds
}