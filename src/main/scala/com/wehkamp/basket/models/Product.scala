package com.wehkamp.basket.models

case class Product(id: String, name: String, description: String, price: BigDecimal, var stock: Long)

