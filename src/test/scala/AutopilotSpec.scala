import akka.actor._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.jraman.avionics.{Autopilot, Pilot, Plane, PilotProvider}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}
import scala.concurrent.duration._


object AutopilotSpec {
  import Plane._
  val configStr =
    s"""
      |akka {
      |  loglevel = DEBUG
      |    actor {
      |      debug {
      |        lifecycle = on
      |      }
      |    }
      |}
    """.stripMargin

  class FakePlane(testCopilot: ActorRef) extends Actor {
    def receive = {
      case GetActor("copilot") =>
        sender ! PlaneActorReference(testCopilot)
    }
  }
}


class AutopilotSpec
  extends TestKit(ActorSystem("AutopilotSpec", ConfigFactory.parseString(AutopilotSpec.configStr)))
  with ImplicitSender
  with WordSpecLike
  with Matchers {

  val testCopilot = TestProbe()
  val testPlane = TestProbe()
  val autopilot = system.actorOf(Props(new Autopilot(testPlane.ref)), "TestAutopilot")

  "Autopilot" should {
    "take control of plane when copilot dies" in {
      autopilot ! Pilot.ReadyToGo
      Thread.sleep(100)   // wait for actors to get setup
      testPlane.expectMsgPF(500.millis) {
        case Plane.GetActor(str) =>
          if (str.toLowerCase == "copilot") {
            testPlane.lastSender ! Plane.PlaneActorReference(testCopilot.ref)
          }
      }
      testCopilot.ref ! PoisonPill
      testPlane.expectMsg(Plane.GiveMeControl)
      testPlane.lastSender shouldBe autopilot
    }
  }

}
