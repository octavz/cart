package com.wehkamp.basket.messages

import com.wehkamp.basket.models.BasketItem

case class AddProduct(userId: String, item: BasketItem)

