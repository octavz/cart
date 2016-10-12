package com.wehkamp.basket.messages

import com.wehkamp.basket.models.BasketItem

case class AddProduct(basketId: String, item: BasketItem) {

}
