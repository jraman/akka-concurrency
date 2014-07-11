import akka.actor.{Props, Actor, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestLatch, TestProbe, TestKit}
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
 * fishForMessage keeps receiving messages (for testActor) so long as the partial function
   matches and returns false or the timeout (with default) is exceeded.
 * Actor.emptyBehavior does nothing but is composable (i.e. not defined for any case).
 * EventSourceSpy overrides sendEvent and hence obviates the need to register a listener.
   Hence, tests that use actor() don't register a listener.
 * Gotcha: Since it is a single ActorSystem for the life of the test, any actor that is
   created will live on and potentially continue to process messages.  So if you have
   two tests, then the altitude values from the second test may be surprising.
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
       * Since we override sendEvent, the question of registering a listener doesn't arise.
       */
      val (ref, _) = actor()
      Await.ready(EventSourceSpy.latch, 1.second)
      EventSourceSpy.latch.isOpen should be (true)      // redundant
    }

    "calculate altitude changes" in new Helper {
      /*
        1. Register a testActor as a listener.
        2. Change the rate of climb to 1.0f
        3. Verify that the testActor receives an altitude > 0
        4. Stop the actor.  Otherwise this actor will live on and process messages from other tests.
       * Note that there are two variations to the sequencing:
         RegisterListener(testActor) RateChange(1f) Tick   (no Tick in between)
         RegisterListener(testActor) Tick [Tick] RateChange(1f) Tick    (one or more Tick in between)
       */
      val ref = system.actorOf(Props(Altimeter()))
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1.0f)
      fishForMessage(500.millis, "Altitude update failed") {
        case AltitudeUpdate(altitude) if altitude > 0.0f =>
          info(s"Fishing: Altitude update: $altitude")
          true
        case AltitudeUpdate(altitude) =>
          info(s"Fishing: Altitude update: $altitude")
          false
      }
      ref ! Stop
      // @todo wait for actor to terminate
    }

    "calculate altitude changes 2" in new Helper {
      /*
       * Reimplementation of above using ignore and expectMsgPF
       * We'll test that multiple messages are received.
       * Since we are dealing with a single ActorSystem, this actor may be the
         same as the (already climbing) actor in a previous test.
       * We switch to using a TestProbe since we are not limited to a single
         instance like in the case of testActor.
      */
      val ref = system.actorOf(Props(Altimeter()))
      val probe = TestProbe()
      ref ! EventSource.RegisterListener(probe.ref)
      ref ! RateChange(0.5f)

      // Ignore any Ticks prior to RateChange taking effect.
      probe.ignoreMsg {
        case AltitudeUpdate(altitude) if altitude == 0.0f =>
          info("Ignored: Altitude update zero")
          true
      }

      for { i <- 1 to 4} {
        probe.expectMsgPF(500.millis) {
          case AltitudeUpdate(altitude) if altitude >= 0.0f =>
            info(s"Expected: Altitude update $altitude")
            true
        }
      }

      ref ! Stop
      // @todo wait for actor to terminate
    }

  }
}
