package com.jraman.investigation

import akka.actor.{PoisonPill, ActorSystem, Props, Actor}

/**
 * become example
 */

object HappyAngryMain extends App {
  val system = ActorSystem("HappyAngrySystem")
  val tester = system.actorOf(Props[Tester], "Tester")

  tester ! "run"

  Thread.sleep(1000)
  system.shutdown()
}


class Tester extends Actor {
  def receive = {
    case "run" => run()
    case msg => println(msg)
  }

  def run() = {
    val puppet = context.actorOf(Props[HappyAngry], "Puppet")

    val states = List(
      "be happy",
      "be happy",
      "be angry",
      "be angry",
      "be happy"
    )

    for (state <- states) {
      println(s"Command: $state")
      puppet ! state
    }

    context.stop(puppet)
  }

}

class HappyAngry extends Actor {
  import context._
  def angry: Receive = {
    case "be angry" => sender() ! "I am already angry :*"
    case "be happy" => {
      become(happy)
      sender() ! "I'm happy"
    }
  }

  def happy: Receive = {
    case "be happy" => sender() ! "I am already happy :)"
    case "be angry" => {
      become(angry)
      sender() ! "I'm angry"
    }
  }

  def receive = {
    case "be angry" => {
      become(angry)
      sender() ! "I'm angry"
    }
    case "be happy" => {
      become(happy)
      sender() ! "I'm happy"
    }
  }
}
