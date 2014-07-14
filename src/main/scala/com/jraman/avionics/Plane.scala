package com.jraman.avionics

import akka.actor.{ActorRef, Props, Actor, ActorLogging}

/**
 * Actor paths and naming convention:
 * /user/Plane
 * /user/Plane/Pilot.<name>
 * /user/Plane/Copilot.<name>
 * /user/Plane/Autopilot
 */


object Plane {
  // Returns the control surface to the Actor that
  // asks for them
  case object GiveMeControl
  // Response to the GiveMeControl message
  case class Controls(controls: ActorRef)
}

// We want the Plane to own the Altimeter and we're going to
// do that by passing in a specific factory we can use to
// build the Altimeter
class Plane extends Actor with ActorLogging {
  import Altimeter._
  import EventSource._
  import Plane._

  val altimeter = context.actorOf(
    Props(Altimeter()), "Altimeter")
  val controls = context.actorOf(
    Props(new ControlSurfaces(altimeter)), "ControlSurfaces")
  val cfgstr = "com.jraman.avionics.flightcrew"
  val config = context.system.settings.config
  val pilot = context.actorOf(Props[Pilot], "Pilot." + config.getString(s"$cfgstr.pilotName"))
  val copilot = context.actorOf(Props[CoPilot], "Copilot." + config.getString(s"$cfgstr.copilotName"))
  val autopilot = context.actorOf(Props[Autopilot], "Autopilot")
  val flightAttendant = context.actorOf(Props(LeadFlightAttendant()),
                                        config.getString(s"$cfgstr.leadAttendantName"))

  override def preStart() = {
    // Register self to receive altimeter updates
    altimeter ! RegisterListener(self)
    List(pilot, copilot) foreach (_ ! Pilot.ReadyToGo)
  }

  def receive = {
    case GiveMeControl =>
      log info("Plane giving control.")
      sender ! Controls(controls)

    case AltitudeUpdate(altitude) =>
      log info(s"Altitude is now: $altitude")
  }
}
