import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import akka.testkit.{ImplicitSender, TestKit}
import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.{fixture, ParallelTestExecution, Matchers}

/**
 * Sec 6.5
 * This is a slight digression into making tests run in parallel.
 * Here we show how to create a separate (new) ActorSystem for each test so that
   they can be run in parallel.  Otherwise, two parallel tests both trying to
   system.actorOf(Props[MyActor], "MyActor") will fail since the actor name has to be
   unique in that actor system.
 * My modifications:
   - The first test "throw when made with wrong name" does not throw an Exception
     with the given code from the book.
 * http://doc.scalatest.org/2.2.0/index.html#org.scalatest.fixture.NoArg has this code and
   als has equivalent code that does not use NoArg.
 * There is quite a bit of magic going on in this code:
   - ActorSys is a fixture which requires no args.
   - ActorSys has an apply method - hence, ActorSys() with no args is possible.
   - Each test constructs new ActorSys with no args - because ActorSys mixes NoArg
   - MyActorSpec extends fixture.WordSpec.  Hence, a fixture object (ActorSys) is passed
     into each test.  Normally, fixture.WordSpec requires defining FixtureParam and withFixture,
     but because of NoArg, the fixture is directly passed in to each test.
   - MyActorSpec mixes in ParallelTestExecution which enable parallel execution of tests.
   - UnitFixture is used in this example, because in this case, the fixture.WordSpec feature enabling
     tests to be defined as functions from fixture objects of type FixtureParam to Unit is not being used.
     Rather, only the secondary feature that enables tests to be defined as functions from no parameters
     to Unit is being used. This secondary feature is described in the second-to-last paragraph on the
     main Scaladoc documentation of fixture.WordSpec, which says:
     If a test doesn't need the fixture, you can indicate that by providing a no-arg instead of a
     one-arg function, ... In other words, instead of starting your function literal with something like
     “db =>”, you'd start it with “() =>”. For such tests, runTest will not invoke withFixture(OneArgTest).
     It will instead directly invoke withFixture(NoArgTest).
     [Above copied from http://doc.scalatest.org/2.2.0/index.html#org.scalatest.fixture.NoArg]
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


// the fixture to be passed into each fixture.WordSpec test
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
