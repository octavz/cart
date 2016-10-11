package com.wehkamp.basket

object Catalog {
  private val catalog = Seq.range(0, 100).map(
    i => (i.toString, Product(id = s"id_$i"
      , name = s"name $i"
      , description = s"description $i"
      , price = 0.5 * i
      , stock = 100))).toMap


  def apply() = catalog

  def remove(productId: String): Long = {
    if (!catalog.contains(productId)) throw new NotFoundException("Product not found")
    val prod = catalog(productId)
    if (prod.stock == 0) throw new StockEmptyException("No more products in stock")
    prod.stock = prod.stock - 1
    prod.stock
  }

  def add(productId: String): Long = {
    if (!catalog.contains(productId)) throw new NotFoundException("Product not found")
    val prod = catalog(productId)
    if (prod.stock == 0) throw new StockEmptyException("No more products in stock")
    prod.stock = prod.stock + 1
    prod.stock
  }
}
