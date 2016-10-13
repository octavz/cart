package com.wehkamp.basket

import akka.stream.ActorMaterializer
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import com.wehkamp.basket.repositories.MemoryRepository
import com.wehkamp.basket.services.BasketActor
import com.wehkamp.basket.utils.{Config, HasActorSupport}

object Main extends App with Config with Routes with HasActorSupport {
  implicit val actorSystem = ActorSystem("shopping-basket")
  override val basketActor = actorSystem.actorOf(Props(new BasketActor(new MemoryRepository)))

  protected implicit val executor: ExecutionContext = actorSystem.dispatcher
  protected val log: LoggingAdapter = Logging(actorSystem, getClass)
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  Http().bindAndHandle(handler = logRequestResult("log")(routes), interface = httpInterface, port = httpPort)

}