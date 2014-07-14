package com.jraman.avionics

import akka.actor.{ActorLogging, Actor, ActorRef}
import com.jraman.avionics.Plane.Controls

/**
 * Pilot
 */

object Pilot {
  // since there are no params we create case objects instead of case classes
  case object ReadyToGo
  case object RelinquishControl
}


class Pilot extends Actor {
  import Pilot._
  import Plane.GiveMeControl

  var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters

  val copilotName = context.system.settings.config.getString(
    "com.jraman.avionics.flightcrew.copilotName"
  )

  def receive = {
    case ReadyToGo =>
      context.parent ! GiveMeControl
      copilot = context.actorFor("../" + copilotName)
      autopilot = context.actorFor("../Autopilot")

    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}


class CoPilot extends Actor {
  import Pilot.ReadyToGo

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters

  val pilotName = context.system.settings.config.getString(
    "com.jraman.avionics.flightcrew.pilotName"
  )

  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
      autopilot = context.actorFor("../Autopilot")
  }
}


class Autopilot extends Actor with ActorLogging {
  // empty for now
  def receive = {
    case msg =>
      log.info(s"Got $msg")
  }
}