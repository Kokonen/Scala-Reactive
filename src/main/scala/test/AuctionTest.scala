package test

import bank._
import bank.Messages._

import akka.actor._
import akka.testkit.{ TestProbe, ImplicitSender, TestActorRef, TestKit }
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import scala.concurrent.duration.`package`.DurationInt

class AuctionTest extends TestKit(ActorSystem("AuctionHouseTest"))
    with WordSpecLike with Matchers with OneInstancePerTest {

  val AUCTION_TEST_NAME = "name"

  val parentTestProbe = TestProbe()
  val auctionSearchTestProbe = TestProbe()

  system.actorOf(Props(new Actor() {
    override def receive = {
      case x => auctionSearchTestProbe.ref forward x
    }
  }), MasterSearch.MASTER_SEARCH_NAME)

  val underTest = TestActorRef(Props(classOf[Auction], AUCTION_TEST_NAME, AUCTION_TEST_NAME, Seller.BID_TIME, 0.0), parentTestProbe.ref, AUCTION_TEST_NAME)

  "An auction" must {

    "register in auction search" in {
      auctionSearchTestProbe.expectMsg(1 second, RegisterAuction(AUCTION_TEST_NAME))
    }

    "be terminated after delete timer expiration" in {
      val probe = TestProbe()
      probe watch underTest
      probe.expectTerminated(underTest, Seller.BID_TIME + (1 second) + Auction.DELETE_TIME)
    }

    "inform winner that he won" in {
      val buyer = TestProbe()
      buyer.send(underTest, Bid(1))
      buyer.expectMsg(Seller.BID_TIME + (1 second), AuctionWon)
    }

    "inform parent that item was sold" in {
      val buyer = TestProbe()
      buyer.send(underTest, Bid(1))
      parentTestProbe.expectMsg(Seller.BID_TIME + (1 second), AuctionSold)
    }

    "tell parent that sold auction was deleted" in {
      val buyer = TestProbe()
      buyer.send(underTest, Bid(1))
      parentTestProbe.expectMsg(Seller.BID_TIME + (1 second), AuctionSold)
      parentTestProbe.expectMsg(Seller.BID_TIME + (1 second) + Auction.DELETE_TIME, AuctionDeleted)
    }

    "tell parent that not sold auction was deleted" in {
      parentTestProbe.expectMsg(Seller.BID_TIME + (1 second) + Auction.DELETE_TIME, AuctionDeleted)
    }

    "tell current winner that he was outbidded" in {
      val buyer = TestProbe()
      val buyer2 = TestProbe()
      buyer.send(underTest, Bid(1))
      buyer2.send(underTest, Bid(2))
      buyer.expectMsg(1 second, Outbidded(2))
    }

    "tell buyer that his bid was too low" in {
      val buyer = TestProbe()
      val buyer2 = TestProbe()
      buyer.send(underTest, Bid(2))
      buyer2.send(underTest, Bid(1))
      buyer2.expectMsg(1 second, BidTooLow(2))
    }

    "tell buyer that item was sold if he bids after timer" in {
      val buyer = TestProbe()
      val buyer2 = TestProbe()
      buyer.send(underTest, Bid(1))
      Thread sleep (Seller.BID_TIME + (1 second)).length * 1000
      buyer2.send(underTest, Bid(2))
      buyer2.expectMsg(1 second, AlreadySold)
    }

  }
}
