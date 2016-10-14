package com.wehkamp.basket.services

import akka.actor.{Actor, ActorLogging}
import com.wehkamp.basket.dtos.{BasketDTO, ProductDTO}
import com.wehkamp.basket.exceptions.{NotFoundException, StockException}
import com.wehkamp.basket.messages.{AddProduct, GetBasket, RemoveProduct}
import com.wehkamp.basket.repositories.Repository
import com.wehkamp.basket.models.{UserData, BasketItem, WProduct}

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

  def validateProductId[T](productId: String)(f: => ServiceResponse[T]): ServiceResponse[T] =
    if (!repository.productExists(productId)) {
      ServiceResponse(404, "No product found")
    } else f

  private def internalGetBasket(userId: String) =
    repository
      .getBasketByUserId(userId)
      .getOrElse(repository.persistBasket(userId, UserData(userId)))

  private[services] def toProductDTO(p: WProduct): ProductDTO =
    ProductDTO(id = p.id, name = p.name, description = p.description, price = p.price)

  private[services] def toDto(b: UserData): BasketDTO = {
    val prods = repository
      .getProductsById(b.items.map(_.productId))
      .map(toProductDTO)
    BasketDTO(b.items, prods)
  }

  def getBasketById(userId: String): ServiceResponse[BasketDTO] =
    authorizeAndCatchErrors(userId) {
      ServiceResponse(
        toDto(internalGetBasket(userId))
      )
    }

  def addProduct(userId: String, item: BasketItem): ServiceResponse[BasketDTO] =
    authorizeAndCatchErrors(userId) {
      validateProductId(item.productId) {
        //ideally, these operations should be enclosed in a transaction
        ServiceResponse {
          repository.decreaseStock(item)
          toDto(repository.persistBasket(userId, internalGetBasket(userId).add(item)))
        }
      }
    }

  def removeProduct(userId: String, productId: String): ServiceResponse[BasketDTO] =
    authorizeAndCatchErrors(userId) {
      validateProductId(productId) {
        val basket = internalGetBasket(userId)
        basket.items.find(_.productId == productId) match {
          case Some(item) =>
            //ideally, these operations should be enclosed in a transaction
            ServiceResponse {
              repository.increaseStock(productId)
              toDto(repository.persistBasket(userId, basket.remove(productId)))
            }
          case _ =>
            ServiceResponse(404, "Product not found in basket")
        }
      }
    }

  override def receive: Receive = {
    case GetBasket(userId) =>
      sender ! getBasketById(userId)
    case AddProduct(userId, item) =>
      sender ! addProduct(userId, item)
    case RemoveProduct(userId, productId) =>
      sender ! removeProduct(userId, productId)
    case "ping" =>
      sender ! "pong"
    case unknown =>
      log.error(s"Unknown message received in BasketActor: $unknown")
  }
}
