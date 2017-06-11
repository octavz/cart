package com.wehkamp.basket.auth

import akka.http.scaladsl.model.RemoteAddress

object AuthHeaders {

  object AuthShopperHeader {
    val Name = "Blaze-Auth-Shopper"
  }

  object AuthAnonymousShopperHeader {
    val Name = "Blaze-Auth-Pre-Authentication-Shopper"
  }

  object AuthLevelHeader {
    val Name = "Blaze-Auth-Level"
  }

  object AuthTokenIdHeader {
    val Name = "Blaze-Auth-TokenId"
  }

  case class Identity(shopperId: String, anonymousShopperId: Option[String], authLevel: String, tokenId: String, clientIpAddress: RemoteAddress)
}
