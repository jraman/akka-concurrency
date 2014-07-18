package com.jraman.avionics

import akka.actor.{ActorSelection, ActorLogging, Actor, ActorRef}
import com.jraman.avionics.Plane.Controls
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global


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
  case object FlyThePlane
}


// Add dependency injection by passing in plane, autopilot, controls, and altimeter
// Why not also provide copilot as arg?  Well, then copilot needs to know about pilot
// and that creates a circular dependency.
class Pilot(plane: ActorRef,
            autopilot: ActorRef,
            var controls: ActorRef,
            altimeter: ActorRef) extends Actor with ActorLogging {
  import Pilot._

  var copilot: ActorSelection = context.actorSelection("/does/not/exist")

  val copilotName = context.system.settings.config.getString(
    "com.jraman.avionics.flightcrew.copilotName"
  )

  def receive = {
    case ReadyToGo =>
      log info "received ReadyToGo"
      copilot = context.actorSelection(copilotName)
      log info s"copilot is $copilot"
    case FlyThePlane =>
      log info "received FlyThePlane"
      flyThePlane()
    case msg =>
      log error s"Unhandled message $msg"
  }

  def flyThePlane() {
    // implicit timeout for ask
    implicit val timeout = Timeout(5.seconds)
    // Handing off control is not fully figured out - anybody can query
    // the actor system for the control system and send messages to it.
    // For now, block to get control.
    val Controls(control) = Await.result(
      (plane ? Plane.GiveMeControl).mapTo[Controls], 5.seconds)

    // Takeoff!
    context.system.scheduler.scheduleOnce(200.millis) {
      control ! ControlSurfaces.StickBack(1f)
    }
    // Level out
    context.system.scheduler.scheduleOnce(1.seconds) {
      control ! ControlSurfaces.StickBack(0f)
    }
    // Climb
    context.system.scheduler.scheduleOnce(3.seconds) {
      control ! ControlSurfaces.StickBack(0.5f)
    }
    // Level out
    context.system.scheduler.scheduleOnce(4.seconds) {
      control ! ControlSurfaces.StickBack(0f)
    }
  }
}


class Copilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef)
  extends Actor with ActorLogging {
  import Pilot.ReadyToGo

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorSelection = context.actorSelection(context.system.deadLetters.path.name)

  val pilotName = context.system.settings.config.getString(
    "com.jraman.avionics.flightcrew.pilotName"
  )

  def receive = {
    case ReadyToGo =>
      pilot = context.actorSelection(pilotName)
    case msg =>
      log error s"Unhandled message $msg"
  }
}


class Autopilot extends Actor with ActorLogging {
  // empty for now
  def receive = {
    case msg =>
      log.info(s"Got $msg")
  }
}
