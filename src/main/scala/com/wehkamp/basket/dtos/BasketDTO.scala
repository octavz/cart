package com.wehkamp.basket.dtos

import com.wehkamp.basket.models.BasketItem

case class BasketDTO(content: Set[BasketItem], products: Set[ProductDTO])

