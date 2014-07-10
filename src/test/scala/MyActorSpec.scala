import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import akka.testkit.{ImplicitSender, TestKit}
import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.{fixture, ParallelTestExecution, Matchers}

/**
 * Sec 6.5 This is a slight digression into making tests run in parallel.
 * Here we show how to create a separate (new) ActorSystem for each test so that
   they can be run in parallel.  Otherwise, two parallel tests both trying to
   system.actorOf(Props[MyActor], "MyActor") will fail since the actor name has to be
   unique in that actor system.
 * My modifications:
   - The first test "throw when made with wrong name" does not throw an Exception
     with the given code from the book.
   -
 */


// The actor messages
case class Ping()
case class Pong()


// The actor to be tested
class MyActor extends Actor {
  override def receive = {
    case Ping => sender ! Pong
  }
}


object ActorSys {
  val uniqueId = new AtomicInteger()
}


class ActorSys(name: String)
  extends TestKit(ActorSystem(name))
  with ImplicitSender
  with fixture.NoArg {

  def this() = this("TestSystem%05d".format(ActorSys.uniqueId.getAndIncrement))

  def shutdown() = system.shutdown()      // system is an implicit val

  override def apply() {
    try super.apply()       // uses NoArg
    finally shutdown()
  }
}


class MyActorSpec
  extends fixture.WordSpec
  with Matchers
  with fixture.UnitFixture
  with ParallelTestExecution {

  def makeActor(system: ActorSystem): ActorRef = system.actorOf(Props[MyActor], "MyActor")

  "My Actor" should {
    "throw when made with the wrong name" in new ActorSys {
      an [Exception] should be thrownBy {
        val a = system.actorOf(Props[MyActor], "MyActor")
        val b = system.actorOf(Props[MyActor], "MyActor")
      }
    }
  }

  "construct without exception" in new ActorSys {
    info(s"system.name = ${system.name}")
    val a = makeActor(system)       // implicit system of TestKit
    // if constructor throws, then this test will fail
  }

  "respond with a Pong to a Ping" in new ActorSys {
    info(s"system.name = ${system.name}")
    val a = makeActor(system)
    a ! Ping
    expectMsg(Pong)
  }
}
