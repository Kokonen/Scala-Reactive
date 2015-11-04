package test

import akka.actor._
import bank._
import bank.Messages._
import akka.testkit.{ TestProbe, ImplicitSender, TestActorRef, TestKit }
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import scala.concurrent.duration.`package`.DurationInt

class AuctionSearchTest extends TestKit(ActorSystem("AuctionHouseTest"))
    with WordSpecLike with Matchers with OneInstancePerTest {
  val NAME_A = "a b c d"
  val NAME_B = "d e f g"
  val REQUEST = "d"

  val underTest = TestActorRef(Props(classOf[AuctionSearch]), "AuctionSearch")

  "An auction search " should {
    "send list with two elements" when {
      "there are two registered auctions and request matches both" in {
        val auction1 = TestProbe()
        auction1.send(underTest, RegisterAuction(NAME_A))
        val auction2 = TestProbe()
        auction2.send(underTest, RegisterAuction(NAME_B))
        val testProbe = TestProbe()
        testProbe.send(underTest, Search(REQUEST))
        testProbe.expectMsg(1 second, SearchResults(List(auction1.ref, auction2.ref)))
      }
    }
    "send list with one element" when {
      "there is one auction registered and request is matching" in {
        val auction = TestProbe()
        auction.send(underTest, RegisterAuction(NAME_A))
        val testProbe = TestProbe()
        testProbe.send(underTest, Search(NAME_A))
        testProbe.expectMsg(1 second, SearchResults(List(auction.ref)))
      }
    }

    "send empty list" when {

      "there is one auction registered and request is not matching" in {
        val auction = TestProbe()
        auction.send(underTest, RegisterAuction(NAME_A))
        val testProbe = TestProbe()
        testProbe.send(underTest, Search(NAME_B))
        testProbe.expectMsg(1 second, SearchResults(List()))
      }

      "there is no registered auction" in {
        val testProbe = TestProbe()
        testProbe.send(underTest, Search(NAME_A))
        testProbe.expectMsg(1 second, SearchResults(List()))
      }
    }
  }
}
