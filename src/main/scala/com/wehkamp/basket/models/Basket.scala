package com.wehkamp.basket.models

import com.wehkamp.basket.exceptions.NotFoundException

case class Basket(basketId: String, items: Set[BasketItem] = Set.empty) {
  def remove(productId: String) =
    items.find(_.productId == productId) match {
      case Some(p) =>
        Basket(basketId = basketId,
          items =
            if (p.quantity > 1) items.filterNot(p == _) + p.copy(quantity = p.quantity - 1)
            else items.filter(p == _)
        )
      case _ =>
        throw new NotFoundException("Item not found in basket")
    }

  def add(item: BasketItem) = {
    items.find(_.productId == item.productId) match {
      case Some(p) => copy(items = items.filterNot(p == _) + p.copy(quantity = p.quantity + item.quantity))
      case _ => copy(items = items + item)
    }
  }

}

