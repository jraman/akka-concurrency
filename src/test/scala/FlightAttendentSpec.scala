import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import com.jraman.avionics.FlightAttendant.{Drink, GetDrink}
import com.jraman.avionics.{AttendantResponsiveness, FlightAttendant}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}

/**
 * Notes & Highlights:
 * FlightAttendant mixing in AttendantResponsiveness is advantageous since we can lower that
   value for testing to be quick.
 * Default scheduler tick duration is 100ms.  We lower it to 10ms for this test.
 */


object FlightAttendentSpec{
  val system = ActorSystem("FlightAttendentSpec",
                           ConfigFactory.parseString("akka.scheduler.tick-duration = 10ms"))
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 10
  }
}


class FlightAttendentSpec
  extends TestKit(FlightAttendentSpec.system)
  with ImplicitSender
  with WordSpecLike
  with Matchers {
  "FlightAttendant" should {
    "get a drink when asked" in {
      val actor = TestActorRef(Props(FlightAttendentSpec()))
      actor ! GetDrink("Soda")
      expectMsg(Drink("Soda"))
    }
  }
}

