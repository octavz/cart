package com.wehkamp.basket.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.wehkamp.basket.utils.Config
import com.wehkamp.basket.{Basket, BasketItem, ServiceActors}
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._

trait BasketApi extends SprayJsonSupport with DefaultJsonProtocol {
  this: ServiceActors with Config =>
  implicit val askTimeout = Timeout(10.seconds)

  implicit val itemFormat = jsonFormat2(BasketItem)
  implicit val basketFormat = jsonFormat2(Basket)

  val basketRoutes =
    get {
      //      basketActor.ask(GetBasket("a")) map {
      //        case Right(b) => complete(StatusCodes.Created -> b.asInstanceOf[Basket])
      //        case Left((code, msg)) => complete(HttpResponse(status = StatusCodes.BadRequest, entity = msg.toString))
      //      }

      pathSingleSlash {
        complete("test")
      }
    } ~
      post {
        path("product") {
          complete("test")
        }
      } ~
      delete {
        path("product" / Segment) { productId =>
          complete("delete_" + productId)
        }
      }
}
