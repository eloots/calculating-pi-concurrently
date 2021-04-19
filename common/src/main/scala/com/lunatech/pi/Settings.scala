package com.lunatech.pi

import com.typesafe.config.{Config, ConfigFactory}

object Settings:
  private val config: Config = ConfigFactory.load()

  val parallelism: Int = config.getInt("calculating-pi.parallelism")
  val piReferenceFile: String = config.getString("calculating-pi.pi-reference-file")
