package com.wehkamp.basket

import akka.http.scaladsl.server.Directives._
import com.wehkamp.basket.api.BasketApi
import com.wehkamp.basket.utils.{Config, SwaggerDocService}

trait Routes extends BasketApi {
  this: ServiceActors with Config =>
  val routes = pathPrefix("api" / "shoppingbasket") {
    basketRoutes
  } ~
    new SwaggerDocService(actorSystem, materializer).routes ~
    pathPrefix("swagger") {
      get {
        getFromResourceDirectory("META-INF/resources/webjars/swagger-ui/2.2.5")
      }
    }
}