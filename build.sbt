name := """shopping-basket"""

organization := "com.wehkamp.basket"

version := "0.0.1"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.4.11"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.7.2",
    "org.webjars" % "swagger-ui" % "2.2.5",
    "org.webjars" % "jquery" % "2.1.1",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"

  )
}

Revolver.settings


fork in run := true