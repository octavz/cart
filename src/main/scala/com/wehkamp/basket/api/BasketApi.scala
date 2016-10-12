package com.wehkamp.basket.api

import javax.ws.rs.Path

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.wehkamp.basket.messages.GetBasket
import com.wehkamp.basket.utils.Config
import com.wehkamp.basket._
import io.swagger.annotations._
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import akka.pattern.ask

import scala.concurrent.ExecutionContext.Implicits.global
import spray.json._
import DefaultJsonProtocol._

import scala.concurrent.Future
import scala.util._

@Api(value = "/shoppingbasket", produces = "application/json")
@Path("/shoppingbasket")
trait BasketApi extends SprayJsonSupport with DefaultJsonProtocol {
  this: ServiceActors with Config =>
  implicit val askTimeout = Timeout(10.seconds)

  implicit val itemFormat = jsonFormat2(BasketItem)
  implicit val basketFormat = jsonFormat2(Basket)

  implicit def responseFormat[T: JsonWriter] = new JsonWriter[ServiceResponse[T]] {
    def write(o: ServiceResponse[T]) = o.errCode match {
      case Some(code) => JsObject("errCode" -> JsNumber(code), "errMessage" -> JsString(o.errMessage.getOrElse("")))
      case _ => o.entity.get.toJson
    }
  }

  def retrieve[S: JsonWriter](f: String => Future[_]) = headerValueByName("Auth") { userId =>
    onComplete(f(userId)) {
      case Success(v) => v match {
        case ServiceResponse(_, Some(ec), _) =>
          complete(StatusCode.int2StatusCode(ec) -> v.asInstanceOf[ServiceResponse[S]].toJson)
        case ServiceResponse(Some(_), None, _) =>
          complete(StatusCodes.Created -> v.asInstanceOf[ServiceResponse[S]].toJson)
        case _ => complete(StatusCodes.InternalServerError -> "No response")
      }
      case Failure(ex) => complete(StatusCodes.InternalServerError -> ex.getMessage)
    }

  }

  @ApiOperation(value = "Return basket content for an user", notes = "", nickname = "getBasket", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "Auth", value = "user id", required = true,
      dataType = "string", paramType = "header")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return basket", response = classOf[ServiceResponse[Basket]]),
    new ApiResponse(code = 401, message = "Not authorized"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getBasket() = get {
    retrieve[Basket] { userId =>
      basketActor ? GetBasket(userId)
    }
  }

  @ApiOperation(value = "Add product to basket", notes = "", nickname = "addProduct", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "basket item", required = true,
      dataType = "com.wehkamp.basket.BasketItem", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return basket", response = classOf[ServiceResponse[Basket]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  @Path("product")
  def addProduct() =
    path("product") {
      post {
        entity(as[BasketItem]) {
          item =>
            retrieve[Basket] { userId =>
              basketActor ? AddProduct(userId, item)
            }
        }
      }
    }

  val basketRoutes =
    getBasket() ~
      addProduct() ~
      delete {
        path("product" / Segment) {
          productId =>
            complete("delete_" + productId)
        }
      }
}
