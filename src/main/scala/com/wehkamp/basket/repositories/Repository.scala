package com.wehkamp.basket.repositories

import com.wehkamp.basket.models.{Basket, BasketItem}

trait Repository {
  def newId(): String

  def userExists(userId: String): Boolean

  def productExists(productId: String): Boolean

  def getBasketByUserId(userId: String): Option[Basket]

  def decreaseStock(item: BasketItem): Long

  def increaseStock(productId: String): Long

  def persistBasket(userId: String, basket: Basket): Basket

  def removeProductAndPersist(userId: String, productId: String): Basket

}
