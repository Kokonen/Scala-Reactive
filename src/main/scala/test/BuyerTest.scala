package test

import bank._
import bank.Messages._

import akka.actor._
import akka.testkit.{ TestProbe, ImplicitSender, TestActorRef, TestKit }
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import scala.concurrent.duration.`package`.DurationInt

class BuyerTest extends TestKit(ActorSystem("AuctionHouseTest"))
    with WordSpecLike with Matchers with OneInstancePerTest {
  val AUCTION_NAME = "name"

  val auctionSearchTestProbe = TestProbe()
  val auctionTestProbe = TestProbe()
  system.actorOf(Props(new Actor() {
    override def receive = {
      case x => sender ! SearchResults(List(auctionTestProbe.ref)); auctionSearchTestProbe.ref forward x
    }
  }), "AuctionSearch")
  val underTest = TestActorRef(Props(classOf[Buyer], List(AUCTION_NAME), 15.5), "buyer")

  "A buyer" must {

    "search an auction using given name" in {
      auctionSearchTestProbe.expectMsg(1 second, Search(AUCTION_NAME))
    }
    
    "try to bid an auction" in {
      auctionTestProbe.expectMsg(2 second, Bid(10))
    }
    
    "try to rebid an auction when was outbidded" in {
      auctionTestProbe.expectMsg(2 second, Bid(10))
      auctionTestProbe.send(underTest, Outbidded(11))
      auctionTestProbe.expectMsg(2 second, Bid(12))
    }
    
    "try to rebid when close to maximum price" in {
      auctionTestProbe.expectMsg(2 second, Bid(10))
      auctionTestProbe.send(underTest, Outbidded(15.0))
      auctionTestProbe.expectMsg(2 second, Bid(15.5))
    }
    
    "terminate if bought all items" in {
      val testProbe = TestProbe()
      testProbe watch underTest
      auctionTestProbe.expectMsg(2 second, Bid(10))
      auctionTestProbe.send(underTest, AuctionWon)
      testProbe.expectTerminated(underTest, 1 second)
    }
    "terminate if he lost every auction" in {
      val testProbe = TestProbe()
      testProbe watch underTest
      auctionTestProbe.expectMsg(2 second, Bid(10))
      auctionTestProbe.send(underTest, AlreadySold)
      testProbe.expectTerminated(underTest, 1 second)
    }
    "terminate if he doesn't have enough money to continue" in {
      val testProbe = TestProbe()
      testProbe watch underTest
      auctionTestProbe.expectMsg(2 second, Bid(10))
      auctionTestProbe.send(underTest, Outbidded(20))
      testProbe.expectTerminated(underTest, 1 second)
    }
  }
}
