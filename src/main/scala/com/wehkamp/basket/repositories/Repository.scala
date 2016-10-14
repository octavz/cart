package com.wehkamp.basket.repositories

import com.wehkamp.basket.models.{UserData, BasketItem, WProduct}

trait Repository {
  def userExists(userId: String): Boolean

  def productExists(productId: String): Boolean

  def getBasketByUserId(userId: String): Option[UserData]

  def decreaseStock(item: BasketItem): Long

  def increaseStock(productId: String): Long

  def persistBasket(userId: String, basket: UserData): UserData

  def getProductsById(productIds: Set[String]): Set[WProduct]

}
