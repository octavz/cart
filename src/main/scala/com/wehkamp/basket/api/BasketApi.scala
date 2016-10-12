package com.wehkamp.basket.api

import javax.ws.rs.Path

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.wehkamp.basket._
import com.wehkamp.basket.messages.{AddProduct, GetBasket, RemoveProduct}
import com.wehkamp.basket.models.{Basket, BasketItem}
import com.wehkamp.basket.services.ServiceResponse
import com.wehkamp.basket.utils.{Config, HasActorSupport}
import io.swagger.annotations._
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util._

@Api(value = "/shoppingbasket", produces = "application/json")
@Path("/shoppingbasket")
trait BasketApi extends SprayJsonSupport with DefaultJsonProtocol {
  this: HasActorSupport with Config =>
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
    new ApiImplicitParam(name = "Auth", value = "user id", required = true, dataType = "string", paramType = "header", defaultValue = "a")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return basket", response = classOf[Basket]),
    new ApiResponse(code = 401, message = "Not authorized"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  @Path("/")
  def getBasket() = get {
    retrieve[Basket] { userId =>
      basketActor ? GetBasket(userId)
    }
  }

  @ApiOperation(value = "Add product to basket", notes = "", nickname = "addProduct", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "basket item", required = true, dataType = "com.wehkamp.basket.models.BasketItem", paramType = "body"),
    new ApiImplicitParam(name = "Auth", value = "user id", required = true, dataType = "string", paramType = "header", defaultValue = "a")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return basket", response = classOf[Basket]),
    new ApiResponse(code = 401, message = "Not authorized"),
    new ApiResponse(code = 422, message = "Stock error"),
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

  @ApiOperation(value = "Remove product from basket", nickname = "removeProduct", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "productId", value = "product id", required = true, paramType = "path"),
    new ApiImplicitParam(name = "Auth", value = "user id", required = true, dataType = "string", paramType = "header", defaultValue = "a")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return basket", response = classOf[Basket]),
    new ApiResponse(code = 401, message = "Not authorized"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  @Path("product/{productId}")
  def deleteProduct() =
    delete {
      path("product" / Segment) {
        productId =>
          retrieve[Basket] { userId =>
            basketActor ? RemoveProduct(userId, productId)
          }
      }
    }

  def test = get {
    path("test") {
      complete("test")
    }
  }

  val basketRoutes =
//    getBasket() ~
//      addProduct() ~
//      deleteProduct() ~
      test
}
