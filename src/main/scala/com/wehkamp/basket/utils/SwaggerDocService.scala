package com.wehkamp.basket.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka.model.Info
import com.github.swagger.akka.{HasActorSystem, SwaggerHttpService}

import scala.reflect.runtime.{universe => ru}
import com.wehkamp.basket.api.BasketApi

class SwaggerDocService(system: ActorSystem, mat: ActorMaterializer) extends SwaggerHttpService with HasActorSystem {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = mat
  override val apiTypes = Seq(ru.typeOf[BasketApi])
  override val host = "localhost:9911" //the url of your api, not swagger's json endpoint
  override val basePath = "/api"    //the basePath for the API you are exposing
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info(version = "1.0")
}
