import akka.actor.{Props, Actor, ActorSystem}
import akka.testkit.{TestActorRef, TestLatch, ImplicitSender, TestKit}
import com.jraman.avionics.{EventSource, Altimeter}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.Await
import scala.concurrent.duration._


/**
 * Highlights:
 * AltimeterSpec has everything - it is both TestKit and WordSpecLike (i.e. has both
   Actor and ScalaTest parts)
 * Helper is like a fixture.  A new instance is used for each test.
 * TestLatch: has two states: open and closed.  If the counter value is 0, it is open.
   It is instantiated with a count and then counted down.
 * We use a modified Altimeter - rather, an Altimeter with a test EventSource.
   Hence, we define a creator function, slicedAltimeter, and use that to create the
   test actor.  In the simple case, we would create a test actor with TestActorRef[T],
   but here we create with TestActorRef[T](Props(creator)).
 * fishForMessage runs evaluates the passed in partial function repeatedly until either
   the partial function returns true or the (default) timeout is exceeded.
 */

class AltimeterSpec
  extends TestKit(ActorSystem("AltimeterSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  import Altimeter._

  override def afterAll() = { system.shutdown() }

  // Instantiate a Helper class for every test, making it nicely reusable
  class Helper {

    object EventSourceSpy {
      // The latch gives us fast feedback when something happens
      val latch = TestLatch(1)
    }

    trait EventSourceSpy extends EventSource {
      def sendEvent[T](event: T): Unit = EventSourceSpy.latch.countDown()
      // We don't care about processing the received messages, so we'll no-op them.
      def eventSourceReceive = Actor.emptyBehavior
    }

    // Altimeter creator - passed to Props
    def slicedAltimeter = new Altimeter with EventSourceSpy

    // Helper that provides an ActorRef and the underlying actor.
    def actor() = {
      val a = TestActorRef[Altimeter](Props(slicedAltimeter))
      (a, a.underlyingActor)
    }
  }


  "Altimeter" should {
    "record the rate of climb changes" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(1.0f))
      real.rateOfClimb should be (real.maxRateOfClimb)

      real.receive(RateChange(-0.5f))
      real.rateOfClimb should be (-0.5 * real.maxRateOfClimb)
    }

    "keep rate of climb changes within bounds" in new Helper {
      val (_, real) = actor()

      real.receive(RateChange(2.0f))
      real.rateOfClimb should be (real.maxRateOfClimb)

      real.receive(RateChange(-2.0f))
      real.rateOfClimb should be (-real.maxRateOfClimb)
    }

    "send events" in new Helper {
      /* On the first Tick, the Altimeter calls EventSource.sendEvent.
       * Below, we wait for the latch to open.  TestLatch is an Awaitable - it
         has a ready method waits for the latch to open or times out with an Exception.
       * And, for good measure, we again test that the latch is open.
       * akka.testkit.TestLatch is built on top of java.util.concurrent.CountDownLatch
       */
      val (ref, _) = actor()
      Await.ready(EventSourceSpy.latch, 1.second)
      EventSourceSpy.latch.isOpen should be (true)      // redundant
    }

    "calculate altitude changes" in new Helper {
      // 1. Register a testActor as a listener.
      // 2. Change the rate of climb to 1.0f
      // 3. Verify that the testActor receives an altitude > 0
      val ref = system.actorOf(Props(Altimeter()))
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1.0f)
      fishForMessage() {
        case AltitudeUpdate(altitude) if altitude > 0.0f =>
          info(s"Altitude update: $altitude")
          true
        case AltitudeUpdate(altitude) =>
          info(s"Altitude update: $altitude")
          false
      }
    }
  }
}
