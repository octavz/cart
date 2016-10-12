package com.wehkamp.basket

import akka.actor.{Actor, ActorLogging}
import com.wehkamp.basket.messages.GetBasket

import com.wehkamp.basket.Store._

case class ServiceResponse[T](entity: Option[T], errCode: Option[Int] = None, errMessage: Option[String] = None)

object ServiceResponse {
  def apply[T](errCode: Int, errMessage: String): ServiceResponse[T] =
    ServiceResponse[T](None, Some(errCode), Some(errMessage))

  def apply[T](entity: T): ServiceResponse[T] = ServiceResponse[T](Some(entity))
}

class BasketActor extends Actor with ActorLogging {

  def authorizeAndCatchErrors[T](userId: String)(f: => ServiceResponse[T]): ServiceResponse[T] =
    if (!userStore.contains(userId)) ServiceResponse[T](401, "Not authorized")
    else try {
      f
    } catch {
      case nfe: NotFoundException => ServiceResponse(404, nfe.getMessage)
      case see: StockEmptyException => ServiceResponse(422, see.getMessage)
      case e: Throwable => ServiceResponse(500, e.getMessage)
    }

  def validateIds[T](userId: String, productId: String)(f: => ServiceResponse[T]): ServiceResponse[T] =
    if (!catalog.contains(productId)) {
      ServiceResponse(404, "No product found")
    } else if (basketStore.contains(userId)) {
      ServiceResponse(404, "No basket found")
    } else {
      f
    }

  def getBasketById(userId: String): ServiceResponse[Basket] =
    authorizeAndCatchErrors(userId) {
      ServiceResponse {
        if (!basketStore.contains(userId)) {
          val newBasket = Basket(newId())
          basketStore.put(userId, newBasket)
          newBasket
        } else {
          basketStore(userId)
        }
      }
    }

  def addProduct(userId: String, item: BasketItem): ServiceResponse[Basket] =
    authorizeAndCatchErrors(userId) {
      validateIds(userId, item.productId) {
        val basket = basketStore(userId)
        Store.remove(item.productId)
        ServiceResponse(basket.add(item.productId))
      }
    }

  def removeProduct(userId: String, productId: String): ServiceResponse[Basket] =
    authorizeAndCatchErrors(userId) {
      validateIds(userId, productId) {
        val basket = basketStore(userId)
        basket.items.find(_.productId == productId) match {
          case Some(item) =>
            Store.remove(productId)
            ServiceResponse(basket.remove(productId))
          case _ =>
            ServiceResponse(404, "Product not found in basket")
        }
      }
    }

  override def receive: Receive = {
    case GetBasket(userId) => sender ! getBasketById(userId)
    case AddProduct(userId, item) => sender ! addProduct(userId, item)
    case RemoveProduct(userId, productId) => sender ! removeProduct(userId, productId)
    case _ =>
      log.debug("unknown message")
  }
}
