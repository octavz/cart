package com.wehkamp.basket.models

case class WProduct(id: String, name: String, description: String, price: BigDecimal, var stock: Long)

