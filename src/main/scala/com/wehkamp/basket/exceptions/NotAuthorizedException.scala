package com.wehkamp.basket.exceptions

/**
  * Created by ozaharia on 12.10.2016.
  */
class NotAuthorizedException(s: String) extends RuntimeException(s) {
  def this() = this("Not authorized to perform this operation")

}
