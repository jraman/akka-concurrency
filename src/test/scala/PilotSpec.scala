import akka.actor._
import akka.pattern.ask
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.util.Timeout
import com.jraman.avionics._
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}
import scala.concurrent.Await
import scala.concurrent.duration._


class FakePilot extends Actor {
  def receive = {
    case _ =>
  }
}


object PilotSpec {
  val pilotName = "TestPilot"
  val copilotName = "TestCopilot"
  val configStr =
    s"""
      |com.jraman.avionics.flightcrew.pilotName = "$pilotName"
      |com.jraman.avionics.flightcrew.copilotName = "$copilotName"
      |
      |akka {
      |  loglevel = DEBUG
      |    actor {
      |      debug {
      |        lifecycle = on
      |      }
      |    }
      |}
    """.stripMargin
}


class PilotSpec
  extends TestKit(ActorSystem("PilotSpec", ConfigFactory.parseString(PilotSpec.configStr)))
  with ImplicitSender
  with WordSpecLike
  with Matchers {

  import PilotSpec._
  import Plane._

  // dummy actor that's injected as autopilot, controls, and altimeter
  def nilActor: ActorRef = TestProbe().ref

  val testPlane = TestProbe()

  // useful paths
  val pilotPath = s"/user/TestPilots/$pilotName"
  val copilotPath = s"/user/TestPilots/$copilotName"

  def pilotsReadyToGo(): ActorRef = {
    // timeout for ask
    implicit val timeout = Timeout(4.seconds)

    val pilots = system.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        override def childStarter(): Unit = {
          context.actorOf(Props[FakePilot], pilotName)
          context.actorOf(Props(new Copilot(testPlane.ref, nilActor, nilActor)), copilotName)
        }
      }),
      "TestPilots")

    // Wait for mailboxes to be up and running for the children
    Await.result(pilots ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)

    // Tell the  Copilot that it's ready to go
    system.actorSelection(copilotPath) ! Pilot.ReadyToGo

    pilots
  }

  // Test code
  "Copilot" should {
    "take control when the Pilot dies" in {
      pilotsReadyToGo()
      // Wait for the copilot to start & start watching pilot
      Thread.sleep(500)
      // kill the pilot
      system.actorSelection(pilotPath) ! PoisonPill
      // Expect to get control
      testPlane.expectMsg(GiveMeControl)
      // Test type of sender (lastSender is part of TestKit)
      // Ends up being a bit convoluted because of actorSelection.
      // anchorPath == "akka://PilotSpec/" and pathString == "/user/TestPilots/TestPilot"
      testPlane.lastSender.path.toString should be
        system.actorSelection(copilotPath).anchorPath + system.actorSelection(copilotPath).pathString.tail
    }
  }
}
