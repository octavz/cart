package com.wehkamp.basket.auth

import AuthHeaders._
import akka.http.scaladsl.server.{Directive1, Directives}
import akka.shapeless.HNil

trait AuthDirectives extends Directives {
  /**
   * Directive that will try to retrieve the auth headers set by the gateway. If a mandatory header has not been
   * set in the request, this directive will fail the request
   */
  def authHeaders: Directive1[Identity] = {
    val shopper = headerValueByName(AuthShopperHeader.Name)
    val anonymousShopper = optionalHeaderValueByName(AuthAnonymousShopperHeader.Name)
    val level = headerValueByName(AuthLevelHeader.Name)
    val tokenId = headerValueByName(AuthTokenIdHeader.Name)
    val ipAddress = extractClientIP

    val a = shopper & anonymousShopper & level & tokenId & ipAddress
    a tflatMap  {
      case shopperId :: Some(anonymousShopperId) :: authLevel :: tokenId :: clientIp :: HNil if (anonymousShopperId.isEmpty) ⇒ provide(Identity(shopperId, None, authLevel, tokenId, clientIp))
      case shopperId :: anonymousShopperId :: authLevel :: tokenId :: clientIp :: HNil                                       ⇒ provide(Identity(shopperId, anonymousShopperId, authLevel, tokenId, clientIp))
    }

  }

}
