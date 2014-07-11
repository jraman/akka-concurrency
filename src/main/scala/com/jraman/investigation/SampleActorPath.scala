package com.jraman.investigation

import akka.actor.{Props, Actor, ActorSystem}


object SampleActorPath extends App {
  val system = ActorSystem("TheActorSystem")

  val actor = system.actorOf(
    Props(new Actor {
      def receive = Actor.emptyBehavior
    }),
    "AnonymousActor"
  )

  println(actor.path)
  // akka://TheActorSystem/user/AnonymousActor

  println(actor.path.elements.mkString("/", "/", ""))
  // /user/AnonymousActor

  println(actor.path.name)
  // AnonymousActor

  system.shutdown()

}