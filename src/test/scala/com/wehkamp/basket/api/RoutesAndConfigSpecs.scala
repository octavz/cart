package com.wehkamp.basket.api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.testkit.{TestActor, TestProbe}
import com.wehkamp.basket.{Main, Routes}
import com.wehkamp.basket.messages.{AddProduct, GetBasket, RemoveProduct}
import com.wehkamp.basket.models.{UserData, BasketItem}
import com.wehkamp.basket.services.ServiceResponse
import com.wehkamp.basket.utils.{Config, HasActorSupport}
import org.scalatest.{Matchers, WordSpec}
import akka.pattern.ask
import akka.util.Timeout
import com.wehkamp.basket.dtos.{BasketDTO, ProductDTO}

import scala.language.reflectiveCalls
import scala.concurrent.duration._
import scala.reflect.ClassTag

class RoutesAndConfigSpecs
  extends WordSpec
    with Matchers
    with ScalatestRouteTest {

  val testUser = "testUser"

  def basket(prods: String*) = BasketDTO(
    content = prods.map(id => BasketItem(id, 1)).toSet,
    products = prods.map(id => ProductDTO(id, s"name $id", s"desc $id", 12)).toSet)

  implicit val akkaTimeout = Timeout(10.seconds)

  def routes = new Routes with HasActorSupport with Config {

    override val actorMaterializer: ActorMaterializer = materializer
    override val actorSystem: ActorSystem = system

    val probe = new TestProbe(system, "test-basket")
    override val basketActor = probe.ref

    def mockWithMessage[S: ClassTag, T](msg: S, reply: T): Unit =
      probe.setAutoPilot {
        val clazz = implicitly[ClassTag[S]].runtimeClass
        new TestActor.AutoPilot {
          def run(sender: ActorRef, msg: Any) = msg match {
            case m if clazz.isInstance(m) =>
              sender ! ServiceResponse[T](reply)
              TestActor.NoAutoPilot
          }
        }
      }

    def mockWithError(errCode: Int, errMessage: String = ""): Unit =
      probe.setAutoPilot {
        new TestActor.AutoPilot {
          def run(sender: ActorRef, msg: Any) = msg match {
            case _ =>
              sender ! ServiceResponse(errCode, errMessage)
              TestActor.NoAutoPilot
          }
        }
      }

    def mockEmpty(): Unit =
      probe.setAutoPilot {
        new TestActor.AutoPilot {
          def run(sender: ActorRef, msg: Any) = msg match {
            case _ =>
              sender ! ServiceResponse(entity = None, errCode = None, errMessage = None)
              TestActor.NoAutoPilot
          }
        }
      }
  }

  "GET /api/shoppingbasket should return the list of products in the basket" in {
    val r = routes
    r.mockWithMessage(GetBasket(testUser), basket("basket_id"))

    Get("/api/shoppingbasket/") ~> addHeader("Auth", testUser) ~> r.routes ~> check {
      r.probe.expectMsg(0.millis, GetBasket(testUser))
      status.isSuccess()
      val response = responseAs[String]
      assert(response.contains("basket_id"))
    }
  }

  "POST /api/shoppingbasket/product should add a product in the basket" in {
    val r = routes
    r.mockWithMessage(AddProduct(testUser, null), basket("basket_id_post"))
    Post("/api/shoppingbasket/product", HttpEntity(MediaTypes.`application/json`, """{"productId": "1", "quantity": 1}""")) ~> addHeader("Auth", testUser) ~> r.routes ~> check {
      r.probe.expectMsg(0.millis, AddProduct(testUser, BasketItem("1", 1)))
      status.isSuccess()
      val response = responseAs[String]
      assert(response.contains("basket_id_post"))
    }
  }

  "DELETE /api/shoppingbasket/product should delete a product from the basket" in {
    val r = routes
    r.mockWithMessage(RemoveProduct(testUser, null), basket("basket_id_delete"))
    Delete("/api/shoppingbasket/product/id1") ~> addHeader("Auth", testUser) ~> r.routes ~> check {
      r.probe.expectMsg(0.millis, RemoveProduct(testUser, "id1"))
      status.isSuccess()
      val response = responseAs[String]
      assert(response.contains("basket_id_delete"))
    }
  }

  "Sending no auth headers should fail with 401" in {
    val r = routes
    Get("/api/shoppingbasket/") ~> r.routes ~> check {
      status.isFailure()
      status.value === "401"
      val response = responseAs[String]
      assert(response.contains("Auth header not sent."))
    }
  }

  "Returning an error from service actor should be handled accordingly" in {
    val r = routes
    r.mockWithError(402, "error")
    Get("/api/shoppingbasket/") ~> addHeader("Auth", testUser) ~> r.routes ~> check {
      status.isFailure()
      status.value === "402"
      val response = responseAs[String]
      assert(response == """{"errCode":402,"errMessage":"error"}""")
    }
  }

  "Swagger should be served at /swagger/index.html" in {
    val r = routes
    Get("/swagger/index.html") ~> r.routes ~> check {
      val response = responseAs[String]
      status.isSuccess()
      assert(response.contains("Swagger UI"))
    }
  }

  "Config at api-docs should be served at /api-docs/swagger.json" in {
    val r = routes
    Get("/api-docs/swagger.json") ~> r.routes ~> check {
      val response = responseAs[String]
      status.isSuccess()
      assert(response.startsWith("{"))
      assert(response.endsWith("}"))
    }
  }

  "Returning empty response from service actor should return error" in {
    val r = routes
    r.mockEmpty()
    Get("/api/shoppingbasket/") ~> addHeader("Auth", testUser) ~> r.routes ~> check {
      r.probe.expectMsg(0.millis, GetBasket(testUser))
      status.isFailure()
      status.value === "401"
      val response = responseAs[String]
      assert(response.contains("No response"))
    }
  }

  "Main entry point contains valid actorSystem, materialier" in {
    try {
      Main.main(Array.empty[String])
      Main.actorSystem should not be null
      Main.actorMaterializer should not be null
      (Main.basketActor ? "ping").mapTo[String].map(_ === "pong")
    } finally {
      Main.actorSystem.terminate()
    }
  }
}