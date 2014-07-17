package com.jraman.avionics

import scala.concurrent.duration.Duration
import akka.actor.SupervisorStrategy.Decider
import akka.actor.{AllForOneStrategy, OneForOneStrategy, SupervisorStrategy}


trait SupervisorStrategyFactory {
  def makeStrategy(maxNrOfRetries: Int, withinTimeRange: Duration)
                  (decider: Decider): SupervisorStrategy
}


trait OneForOneStrategyFactory
  extends SupervisorStrategyFactory {
  override def makeStrategy(maxNrOfRetries: Int, withinTimeRange: Duration)
                           (decider: Decider): SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries, withinTimeRange)(decider)
}


trait AllForOneStrategyFactory
  extends SupervisorStrategyFactory {
  override def makeStrategy(maxNrOfRetries: Int, withinTimeRange: Duration)
                           (decider: Decider): SupervisorStrategy =
    AllForOneStrategy(maxNrOfRetries, withinTimeRange)(decider)
}
