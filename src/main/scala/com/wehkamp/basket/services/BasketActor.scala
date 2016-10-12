package com.wehkamp.basket.services

import akka.actor.{Actor, ActorLogging}
import com.wehkamp.basket.exceptions.{NotFoundException, StockException}
import com.wehkamp.basket.messages.{AddProduct, GetBasket, RemoveProduct}
import com.wehkamp.basket.repositories.Repository
import com.wehkamp.basket.models.{Basket, BasketItem}

class BasketActor(repository: Repository) extends Actor with ActorLogging {

  def authorizeAndCatchErrors[T](userId: String)(f: => ServiceResponse[T]): ServiceResponse[T] =
    if (!repository.userExists(userId)) ServiceResponse[T](401, "Not authorized")
    else try {
      f
    } catch {
      case nfe: NotFoundException =>
        log.error("BasketActor", nfe)
        ServiceResponse(404, nfe.getMessage)
      case see: StockException =>
        log.error("BasketActor", see)
        ServiceResponse(422, see.getMessage)
      case e: Throwable =>
        log.error("BasketActor", e)
        ServiceResponse(500, e.getMessage)
    }

  def validateIds[T](userId: String, productId: Option[String])(f: Basket => ServiceResponse[T]): ServiceResponse[T] =
    if (productId.isDefined && !repository.productExists(productId.get)) {
      ServiceResponse(404, "No product found")
    } else if (!repository.userExists(userId)) {
      ServiceResponse(404, "No user found")
    } else {
      repository.getBasketByUserId(userId) match {
        case Some(b) => f(b)
        case _ => f(repository.persistBasket(userId, Basket(repository.newId())))
      }
    }

  def getBasketById(userId: String): ServiceResponse[Basket] =
    authorizeAndCatchErrors(userId) {
      validateIds(userId, None) {
        ServiceResponse(_)
      }
    }

  def addProduct(userId: String, item: BasketItem): ServiceResponse[Basket] =
    authorizeAndCatchErrors(userId) {
      validateIds(userId, Some(item.productId)) { basket =>
        repository.decreaseStock(item)
        ServiceResponse {
          repository.addProductAndPersist(userId, item)
        }
      }
    }

  def removeProduct(userId: String, productId: String): ServiceResponse[Basket] =
    authorizeAndCatchErrors(userId) {
      validateIds(userId, Some(productId)) { basket =>
        basket.items.find(_.productId == productId) match {
          case Some(item) =>
            repository.decreaseStock(BasketItem(productId, 1))
            ServiceResponse {
              repository.removeProductAndPersist(userId, productId)
            }
          case _ =>
            ServiceResponse(404, "Product not found in basket")
        }
      }
    }

  override def receive: Receive = {
    case GetBasket(userId) => sender ! getBasketById(userId)
    case AddProduct(userId, item) => sender ! addProduct(userId, item)
    case RemoveProduct(userId, productId) => sender ! removeProduct(userId, productId)
    case unknown => log.error(s"Unknown message received in BasketActor: $unknown")
  }
}
