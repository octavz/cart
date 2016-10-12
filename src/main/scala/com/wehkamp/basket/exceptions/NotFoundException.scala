package com.wehkamp.basket.exceptions

class NotFoundException(s: String) extends RuntimeException(s) {
  def this() = this("Item not found")
}



