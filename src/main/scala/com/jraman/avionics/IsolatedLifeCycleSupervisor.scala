package com.jraman.avionics

import akka.actor.{StopChild, ActorKilledException, ActorInitializationException, Actor}
import scala.concurrent.duration.Duration
import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}


/**
 * Supervisor fault-tolerance plumbing.
 */

object IsolatedLifeCycleSupervisor {
  // Messages: enable waiting for start
  case object WaitForStart
  case object Started
}


/**
 * Supervisor Actor for Resume and Stop
 * See actor hierarchy diagram in Section 8.7
 */
trait IsolatedLifeCycleSupervisor extends Actor {
  import IsolatedLifeCycleSupervisor._

  /**
   * Implement receive for subclasses.  Throw an error for any message received,
     since this is just fault-tolerance plumbing.
   */
  override def receive = {
    case WaitForStart => sender ! Started
    case msg => throw new Exception(s"${self.path.name}: unsupported message: $msg")
  }

  // TBI by subclass
  def childStarter(): Unit

  // Start children only on my start, not restart
  final override def preStart() = { childStarter() }

  // Don't call preStart (as in default behavior)
  final override def postRestart(reason: Throwable) = { /* empty */ }

  // Don't stop children (as in default behavior)
  final override def preRestart(reason: Throwable, message: Option[Any]) = { /* empty */ }
}


/**
 * Supervisor Actor for Altimeter, AutoPilot, and ControlSurfaces.
 * Requires a SupervisorStrategyFactory mixin.
 * @param maxNrOfRetries
 * @param withinTimeRange
 */
abstract class IsolatedResumeSupervisor(
                                         maxNrOfRetries: Int = -1,
                                         withinTimeRange: Duration = Duration.Inf)
  extends IsolatedLifeCycleSupervisor {
  this: SupervisorStrategyFactory =>

  // Declare the decider: Resume on generic exception
  override val supervisorStrategy = makeStrategy(maxNrOfRetries,
    withinTimeRange) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Resume
    case _ => Escalate
  }
}


/**
 * Supervisor Actor for Pilot and CoPilot.
 * Requires a SupervisorStrategyFactory mixin.
 * @param maxNrOfRetries
 * @param withinTimeRange
 */
abstract class IsolatedStopSupervisor(
                                         maxNrOfRetries: Int = -1,
                                         withinTimeRange: Duration = Duration.Inf)
  extends IsolatedLifeCycleSupervisor {
  this: SupervisorStrategyFactory =>

  // Declare the decider: Stop on generic exception
  override val supervisorStrategy = makeStrategy(maxNrOfRetries,
    withinTimeRange) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Stop
    case _ => Escalate
  }
}
