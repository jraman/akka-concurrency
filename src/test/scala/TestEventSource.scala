import akka.actor.{Actor, ActorSystem}
import akka.testkit.{TestKit, TestActorRef}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll}
import org.scalatest.Matchers

import com.jraman.avionics._

/**
 * Notes:
 * TestActorRef[TestEventSource].underlyingActor provides access to the actor itself (not ActorRef).
 */

class TestEventSource extends Actor
  with ProductionEventSource {
  def receive = eventSourceReceive
}


/**
 * Notes:
 * testActor and expectMsg are provided by TestKit
 */
class EventSourceSpec extends TestKit(ActorSystem("EventSourceSpec"))
  with WordSpecLike
  with Matchers       // should contain, not contain, be ...
  with BeforeAndAfterAll {

  import EventSource._

  override def afterAll() = { system.shutdown() }

  "EventSource" should {
    "register a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.listeners should contain (testActor)
    }

    "unregister a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.listeners should contain (testActor)
      real.receive(UnregisterListener(testActor))
      real.listeners should not contain testActor
      real.listeners.size should be (0)
    }

    "send the event to test actor" in {
      val testA = TestActorRef[TestEventSource]
      testA ! RegisterListener(testActor)
      testA.underlyingActor.sendEvent("Fibonacci")
      expectMsg("Fibonacci")
    }
  }

}
