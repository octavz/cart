package com.wehkamp.basket

case class BasketItem(productId: String, quantity: Int)

case class Basket(basketId: String, items: Seq[BasketItem] = Seq.empty) {
  def remove(productId: String) =
    items.find(_.productId == productId) match {
      case Some(p) =>
        Basket(basketId, items = items.filterNot(_.productId == productId))
      case _ =>
        throw new NotFoundException("Item not found in basket")
    }

  def add(productId: String) = Basket(basketId, items :+ BasketItem(productId, 1))

}

