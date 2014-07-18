package com.jraman.avionics

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._
import Plane.Controls

/**
 * Avionics() creates Plane Actor
 * Plane creates Pilot, CoPilot, Autopilot, LeadFlightAttendant, Altimeter, and ControlSurfaces Actors
 * LeadFlightAttendant creates the subordinate FlightAttendant Actors.
 *
 * This can be run in two modes of operation:
   1. Get the controls of the plane and simulate from here.
   2. Message the Pilot to run a simulation.
 * In the first case, the Pilot (and Copilot) are not involved in the operation.
 */

// The futures created by the ask syntax need an
// execution context on which to run, and we will use the
// default global instance for that context
import scala.concurrent.ExecutionContext.Implicits.global


object Avionics {
  // needed for '?' below
  implicit val timeout = Timeout(5.seconds)
  val system = ActorSystem("PlaneSimulation")
  val plane = system.actorOf(
    Props(new Plane() with AltimeterProvider with PilotProvider with LeadFlightAttendantProvider),
    "Plane")

  def main(args: Array[String]) {
    if (args.length == 0) simulate()
    else pilotFlysThePlane()
  }

  def simulate() {
    system.log.info("Simulating flight without the pilot.")
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

  def pilotFlysThePlane() {
    import Pilot.FlyThePlane
    system.log.info("Pilot will start flying the plane.")

    // Wait for the system to initialize and generate the Pilot actor.  Is there a better way?
    // Otherwise, the message ends up in the dead letter box.
    Thread.sleep(200)

    val pilotName = system.settings.config.getString("com.jraman.avionics.flightcrew.pilotName")
    val pilot = system.actorSelection(s"/user/Plane/Pilots/$pilotName")
    system.log debug s"Pilot actor reference: $pilot"

    pilot.tell(Pilot.FlyThePlane, Actor.noSender)
    system.log info s"FlyThePlane sent to $pilot"

    // Shut down
    system.scheduler.scheduleOnce(5.seconds) {
      system.shutdown()
    }
  }
}
