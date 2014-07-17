package com.jraman.avionics

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._
import Plane.Controls

/**
 * main() creates Plane Actor
 * Plane creates Pilot, CoPilot, Autopilot, LeadFlightAttendant, Altimeter, and ControlSurfaces Actors
 * LeadFlightAttendant creates the subordinate FlightAttendant Actors
 */

// The futures created by the ask syntax need an
// execution context on which to run, and we will use the
// default global instance for that context
import scala.concurrent.ExecutionContext.Implicits.global


object Avionics {
  // needed for '?' below
  implicit val timeout = Timeout(5.seconds)
  val system = ActorSystem("PlaneSimulation")
  val plane = system.actorOf(Props(new Plane() with AltimeterProvider with PilotProvider with LeadFlightAttendantProvider), "Plane")

  def main(args: Array[String]) {
    // Grab the controls
    // Future returned by an Actor is of type Any.  Use mapTo to coerce it to ActorRef.
    val Controls(control) = Await.result(
      (plane ? Plane.GiveMeControl).mapTo[Controls], 5.seconds)

    // Takeoff!
    system.scheduler.scheduleOnce(200.millis) {
      control ! ControlSurfaces.StickBack(1f)
    }
    // Level out
    system.scheduler.scheduleOnce(1.seconds) {
      control ! ControlSurfaces.StickBack(0f)
    }
    // Climb
    system.scheduler.scheduleOnce(3.seconds) {
      control ! ControlSurfaces.StickBack(0.5f)
    }
    // Level out
    system.scheduler.scheduleOnce(4.seconds) {
      control ! ControlSurfaces.StickBack(0f)
    }
    // Shut down
    system.scheduler.scheduleOnce(5.seconds) {
      system.shutdown()
    }
  }
}
