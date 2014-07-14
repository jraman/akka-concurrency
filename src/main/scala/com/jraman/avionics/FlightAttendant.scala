package com.jraman.avionics

/**
 * Notes & Highlights:
 * Notice that FlightAttendant does not extend AttendantResponsiveness, rather
   AttendantResponsiveness is injected when instantiating FlightAttendant.
 */


import scala.concurrent.duration._
import akka.actor.Actor


trait AttendantResponsiveness {
  val maxResponseTimeMS: Int
  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis
}


object FlightAttendant {
  case class GetDrink(drinkName: String)
  case class Drink(drinkName: String)

  // By default, we make attendants that respond within 5 minutes
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 300000
  }
}


class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>

  import FlightAttendant._

  implicit val ec = context.dispatcher

  def receive = {
    case GetDrink(drinkName) =>
      context.system.scheduler.scheduleOnce(responseDuration, sender(), Drink(drinkName))
  }
}
