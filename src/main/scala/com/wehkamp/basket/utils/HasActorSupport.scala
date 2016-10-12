package com.wehkamp.basket.utils

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer

trait HasActorSupport {
  val basketActor: ActorRef
  val actorSystem: ActorSystem
  val actorMaterializer: ActorMaterializer

}
