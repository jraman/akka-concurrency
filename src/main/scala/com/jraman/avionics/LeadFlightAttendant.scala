package com.jraman.avionics

/**
 * LeadFlightAttendant
 * Creates subordinate FlightAttendants in preStart() method
   using context.actorOf() with a creator method.
 */

import akka.actor.{Props, ActorRef, Actor}
import com.jraman.avionics.LeadFlightAttendant.GetFlightAttendant



// Policy for creating subordinate flight attendants.
trait AttendantCreationPolicy {
  val numberOfAttendants = 8
  def createAttendant: Actor = FlightAttendant()
}


// Mechanism for creating the LeadFlightAttendant.  For cake pattern.
trait LeadFlightAttendantProvider {
  def newLeadFlightAttendant: Actor = LeadFlightAttendant()
}


object LeadFlightAttendant {
  case object GetFlightAttendant
  case class Attendant(a: ActorRef)
  def apply() = new LeadFlightAttendant with AttendantCreationPolicy
}


class LeadFlightAttendant extends Actor {
  this: AttendantCreationPolicy =>

  import LeadFlightAttendant.Attendant

  override def preStart() {
    import scala.collection.JavaConverters._
    val attendantNames = context.system.settings.config.getStringList("com.jraman.avionics.flightcrew.attendantNames").asScala

    attendantNames take numberOfAttendants foreach {
      // Create flight attendants as children of this actor
      (name) => context.actorOf(Props(createAttendant), name)
    }
  }

  def randomAttendant(): ActorRef = {
    context.children.drop(scala.util.Random.nextInt(numberOfAttendants)).head
  }

  def receive = {
    case GetFlightAttendant => sender ! Attendant(randomAttendant())
    case msg => randomAttendant() forward msg
  }
}


// short program to show construction of actors - lead attendant + subordinate attendants
object FlightAttendantChecker extends App {
  val system = akka.actor.ActorSystem("PlaneSimulation")
  val props = Props(new LeadFlightAttendant with AttendantCreationPolicy)
  val name = system.settings.config.getString("com.jraman.avionics.flightcrew.leadAttendantName")

  val lead = system.actorOf(props, name)
  Thread.sleep(1000)    // give some time for the attendants to get created
  system.shutdown()
}