package com.wehkamp.basket.api

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.testkit._
import com.wehkamp.basket.repositories.MemoryRepository
import com.wehkamp.basket.services.BasketActor
import com.wehkamp.basket.utils.{Config, HasActorSupport}
import org.scalatest.{Matchers, WordSpec}

class BasketApiSpec
  extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with BasketApi
    with HasActorSupport
    with Config {
  override val basketActor = TestActorRef(Props(new BasketActor(new MemoryRepository)))

  override val actorMaterializer: ActorMaterializer = materializer
  override val actorSystem: ActorSystem = system

  "GET /api/shoppingbasket should return the list of products in the basket" in {
    Get("/test") ~> basketRoutes ~> check {
      handled shouldBe true
    }
  }

  //    "POST /api/shoppingbasket/product should add a product in the basket" in {
  //      Post("/product") ~> basketRoutes ~> check {
  //        status.isSuccess()
  //        val response = responseAs[String]
  //        response should be("test")
  //      }
  //    }
  //
  //  "PUT /api/shoppingbasket/product should modify a product in the basket" in {
  //    Put("/product/id") ~> basketRoutes ~> check {
  //      status.isSuccess()
  //      val response = responseAs[String]
  //      response should be("test_id")
  //    }
  //  }
  //
  //  "DELETE /api/shoppingbasket/product should delete a product from the basket" in {
  //    Delete("/product/id1") ~> basketRoutes ~> check {
  //      status.isSuccess()
  //      val response = responseAs[String]
  //      response should be("delete_id1")
  //    }
  //  }
}