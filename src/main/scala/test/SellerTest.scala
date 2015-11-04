package test

import akka.actor._
import bank._
import akka.testkit.{ TestProbe, ImplicitSender, TestActorRef, TestKit }
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import scala.concurrent.duration.`package`.DurationInt
import org.scalatest.words.HaveWord

class SellerTest extends TestKit(ActorSystem("AuctionHouseTest"))
    with WordSpecLike with Matchers with OneInstancePerTest {
  val AUCTION_NAMES = List("a", "b")

  val underTest = TestActorRef(Props(classOf[Seller], AUCTION_NAMES), "seller")

  "A seller" must {
     
    "end when his children end" in {
      val probe = TestProbe()
      probe watch underTest
      probe.expectTerminated(underTest, Seller.BID_TIME + Auction.DELETE_TIME + (1 second))
    }
    
    "have " + AUCTION_NAMES.length + " children" in {
      underTest.children.size equals AUCTION_NAMES.length
    }
  }
}
