package com.wehkamp.basket

import akka.actor.{Actor, ActorLogging}
import com.wehkamp.basket.messages.GetBasket

import scala.collection.mutable

class BasketActor extends Actor with ActorLogging {
  val basketStore = mutable.Map[String, Basket]()

  def newId() = java.util.UUID.randomUUID().toString

  def getBasketById(basketId: String): Either[(Int, String), Basket] = {
    if (!basketStore.contains(basketId)) {
      val newBasket = Basket(basketId = newId())
      basketStore.put(newBasket.basketId, newBasket)
    }
    Right(basketStore(basketId))
  }

  def validateIds(basketId: String, productId: String): Either[(Int, String), Unit] = {
    if (!Catalog().contains(productId)) {
      Left(404, "No product found")
    } else if (basketStore.contains(basketId)) {
      Left(404, "No basket found")
    } else {
      Right(())
    }
  }

  def addProduct(basketId: String, productId: String): Either[(Int, String), Basket] = {
    try {
      validateIds(basketId, productId) match {
        case Right(_) =>
          val basket = basketStore(basketId)
          Catalog.remove(productId)
          Right(basket.add(productId))
        case Left(err) => Left(err)
      }
    } catch {
      case nfe: NotFoundException => Left((404, nfe.getMessage))
      case see: StockEmptyException => Left((422, see.getMessage))
      case e: Throwable => Left((500, e.getMessage))
    }
  }

  def removeProduct(basketId: String, productId: String): Either[(Int, String), Basket] = {
    try {
      validateIds(basketId, productId) match {
        case Right(_) =>
          val basket = basketStore(basketId)
          basket.items.find(_.productId == productId) match {
            case Some(item) =>
              Catalog.remove(productId)
              Right(basket.remove(productId))
            case _ =>
              Left(404, "Product not found in basket")
          }
        case Left(err) => Left(err)
      }
    } catch {
      case nfe: NotFoundException => Left((404, nfe.getMessage))
      case see: StockEmptyException => Left((422, see.getMessage))
      case e: Throwable => Left((500, e.getMessage))
    }
  }

  override def receive: Receive = {
    case GetBasket(id) => sender ! getBasketById(id)
    case AddProduct(basketId, productId) => sender ! addProduct(basketId, productId)
    case RemoveProduct(basketId, productId) => sender ! removeProduct(basketId, productId)
    case _ =>
      log.debug("unknown message")
  }
}
