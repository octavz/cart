package com.wehkamp.basket

import akka.http.scaladsl.server.Directives._
import com.wehkamp.basket.api.BasketApi
import com.wehkamp.basket.utils.Config

trait Routes extends BasketApi {
  this: ServiceActors with Config =>

  val routes = pathPrefix("api" / "shoppingbasket") {
    basketRoutes
  }
}