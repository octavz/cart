package com.wehkamp.basket

class NotFoundException(s: String) extends RuntimeException(s)

class NotAuthorizedException(s: String) extends RuntimeException(s) {
  def this() = this("Not authorized to perform this operation")

}

