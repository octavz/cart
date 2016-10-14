package com.wehkamp.basket.repositories

import com.wehkamp.basket.exceptions.{NotFoundException, StockException}
import com.wehkamp.basket.models.{UserData, BasketItem, WProduct}

class MemoryRepository extends Repository {
  private[repositories] val maxStock = 10
  private[repositories] val basketStore = scala.collection.mutable.Map[String, UserData]()
  private[repositories] val userStore = Set("a", "b", "c", "d")

  private[repositories] val catalog = Seq.range(0, 100).map(
    i => (s"id_$i",
      WProduct(id = s"id_$i"
        , name = s"name $i"
        , description = s"description $i"
        , price = 0.5 * i
        , stock = maxStock))).toMap

  override def userExists(userId: String): Boolean = userStore.contains(userId)

  override def productExists(productId: String): Boolean = catalog.contains(productId)

  override def getBasketByUserId(userId: String): Option[UserData] = basketStore.get(userId)

  override def decreaseStock(item: BasketItem): Long = catalog.get(item.productId) match {
    case Some(p) =>
      if (p.stock == 0) throw new StockException("No more products in stock")
      val newStock = p.stock - item.quantity
      if (newStock < 0) throw new StockException("Not enough products in stock")
      p.stock = newStock
      newStock
    case _ => throw new NotFoundException("Product not found")
  }

  override def increaseStock(productId: String): Long = catalog.get(productId) match {
    case Some(p) =>
      p.stock = p.stock + 1
      p.stock
    case _ => throw new NotFoundException("Product not found")
  }

  override def persistBasket(userId: String, basket: UserData): UserData = {
    if (!userStore.contains(userId)) throw new NotFoundException("User not found")
    basketStore(userId) = basket
    basket
  }

  override def getProductsById(productIds: Set[String]): Set[WProduct] = productIds.flatMap(catalog.get)
}


