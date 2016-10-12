package com.wehkamp.basket.repositories

import com.wehkamp.basket.exceptions.{NotFoundException, StockException}
import com.wehkamp.basket.models.{Basket, BasketItem, Product}

import scala.collection.mutable

class MemoryRepository extends Repository {
  private val basketStore = mutable.Map[String, Basket]()

  private val userStore = Set("a", "b", "c", "d")

  private val catalog = Seq.range(0, 100).map(
    i => (s"id_$i",
      Product(id = s"id_$i"
        , name = s"name $i"
        , description = s"description $i"
        , price = 0.5 * i
        , stock = 10))).toMap

  override def newId(): String = java.util.UUID.randomUUID().toString

  override def userExists(userId: String): Boolean = userStore.contains(userId)

  override def productExists(productId: String): Boolean = catalog.contains(productId)

  override def getBasketByUserId(userId: String): Option[Basket] = basketStore.get(userId)

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
        if (p.stock == 0) throw new StockException("No more products in stock")
        p.stock = p.stock + 1
        p.stock
      case _ => throw new NotFoundException("Product not found")
    }

  override def persistBasket(userId: String, basket: Basket): Basket = {
    basketStore(userId) = basket
    basket
  }

  override def removeProductAndPersist(userId: String, productId: String): Basket = {
    val basket = basketStore(userId)
    persistBasket(userId, basket.remove(productId))
  }

  override def addProductAndPersist(userId: String, item: BasketItem): Basket = {
    val basket = basketStore(userId)
    persistBasket(userId, basket.add(item))
  }

}


