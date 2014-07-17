package com.jraman.avionics

import akka.actor.{ActorLogging, Actor, ActorRef}
import com.jraman.avionics.Plane.Controls

/**
 * Pilot & Copilot
 */

// PilotProvider for cake pattern
trait PilotProvider {
  def newPilot(plane: ActorRef, autopilot: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor =
    new Pilot(plane, autopilot, controls, altimeter)
  def newCopilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef): Actor =
    new Copilot(plane, autopilot, altimeter)
  def newAutopilot: Actor = new Autopilot
}


object Pilot {
  // since there are no params we create case objects instead of case classes
  case object ReadyToGo
  case object RelinquishControl
}


// Add dependency injection by passing in plane, autopilot, controls, and altimeter
// Why not also provide copilot as arg?  Well, then copilot needs to know about pilot
// and that creates a circular dependency.
class Pilot(plane: ActorRef,
            autopilot: ActorRef,
            var controls: ActorRef,
            altimeter: ActorRef) extends Actor {
  import Pilot._

  var copilot: ActorRef = context.system.deadLetters

  val copilotName = context.system.settings.config.getString(
    "com.jraman.avionics.flightcrew.copilotName"
  )

  def receive = {
    case ReadyToGo =>
      copilot = context.actorFor("../" + copilotName)

    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}


class Copilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef) extends Actor {
  import Pilot.ReadyToGo

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters

  val pilotName = context.system.settings.config.getString(
    "com.jraman.avionics.flightcrew.pilotName"
  )

  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
  }
}


class Autopilot extends Actor with ActorLogging {
  // empty for now
  def receive = {
    case msg =>
      log.info(s"Got $msg")
  }
}
