package com.wehkamp.basket

package object services {

  case class ServiceResponse[T](entity: Option[T], errCode: Option[Int] = None, errMessage: Option[String] = None)

  object ServiceResponse {
    def apply[T](errCode: Int, errMessage: String): ServiceResponse[T] =
      ServiceResponse[T](None, Some(errCode), Some(errMessage))

    def apply[T](entity: T): ServiceResponse[T] = ServiceResponse[T](Some(entity))
  }
}
