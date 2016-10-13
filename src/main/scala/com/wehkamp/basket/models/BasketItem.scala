package com.wehkamp.basket.models

import io.swagger.annotations.ApiModelProperty

case class BasketItem(
                       @ApiModelProperty(example = "id_1") productId: String,
                       @ApiModelProperty(example = "1") quantity: Int = 1)
