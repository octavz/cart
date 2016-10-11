package com.wehkamp.basket.api

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit._
import com.wehkamp.basket.utils.Config
import com.wehkamp.basket.{BasketActor, ServiceActors}
import org.scalatest.{Matchers, WordSpec}

class BasketApiSpec
  extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with BasketApi
    with ServiceActors
    with Config {
  override val basketActor = TestActorRef[BasketActor]

  "GET /api/shoppingbasket should return the list of products in the basket" in {
    Get("/") ~> basketRoutes ~> check {
      status.isSuccess()
      val response = responseAs[String]
      response should be("test")
    }

  }

  //  "POST /api/shoppingbasket/product should add a product in the basket" in {
  //    Post("/product") ~> basketRoutes ~> check {
  //      status.isSuccess()
  //      val response = responseAs[String]
  //      response should be("test")
  //    }
  //  }
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