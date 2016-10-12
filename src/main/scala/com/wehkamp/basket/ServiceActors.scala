package com.wehkamp.basket

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer

trait ServiceActors {
  val actorSystem: ActorSystem
  val basketActor: ActorRef
  val materializer: ActorMaterializer

}
