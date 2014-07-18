package com.jraman.avionics

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import akka.util.Timeout
import scala.concurrent.duration._
import com.jraman.avionics.IsolatedLifeCycleSupervisor.WaitForStart
import scala.concurrent.Await
import akka.pattern.ask


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
  this: AltimeterProvider
        with PilotProvider
        with LeadFlightAttendantProvider =>

  import Altimeter._
  import Plane._

  val cfgstr = "com.jraman.avionics.flightcrew"
  val config = context.system.settings.config
  val pilotName = config.getString(s"$cfgstr.pilotName")
  val copilotName = config.getString(s"$cfgstr.copilotName")
  val leadAttendantName = config.getString(s"$cfgstr.leadAttendantName")

  def actorForPilots(name: String) = context.actorFor("Pilots/" + name)

  override def preStart() = {
    import EventSource.RegisterListener
    import Pilot.ReadyToGo

    // Start children.  Order is important.
    startEquipment()
    startPeople()

    // Bootstrap the system
    actorForControls("Altimeter") ! RegisterListener(self)
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo
  }

  def receive = {
    case GiveMeControl =>
      log info s"Plane giving control to ${sender}"
      val controls = context.actorFor("Equipment/ControlSurfaces")
      sender ! Controls(controls)

    case AltitudeUpdate(altitude) =>
      log info(s"Altitude is now: $altitude")
  }

  implicit val askTimeout = Timeout(1.second)

  // Start ResumeSupervisor and children
  def startEquipment() = {
    val controls = context.actorOf(
      Props(new IsolatedResumeSupervisor() with OneForOneStrategyFactory {
        override def childStarter(): Unit = {
          val altimeter = context.actorOf(Props(newAltimeter), "Altimeter")
          // these children get implicitly added
          context.actorOf(Props(newAutopilot), "Autopilot")
          context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")
        }
      }), "Equipment")

    Await.result(controls ? WaitForStart, 1.second)
  }

  def actorForControls(name: String) = context.actorFor("Equipment/" + name)

  // Start StopSupervisor and children
  def startPeople() = {
    val plane = self

    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("Autopilot")
    val altimeter = actorForControls("Altimeter")
    val people = context.actorOf(
      Props(new IsolatedStopSupervisor() with OneForOneStrategyFactory {
        override def childStarter(): Unit = {
          // These children get implicitly added to actor hierarychy
          context.actorOf(Props(newCopilot(plane, autopilot, altimeter)), copilotName)
          context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
        }
      }), "Pilots")

    // use default strategy - with infinite restarts
    context.actorOf(Props(newLeadFlightAttendant), leadAttendantName)

    Await.result(people ? WaitForStart, 1.second)
  }
}
