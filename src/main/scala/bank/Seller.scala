package bank

import akka.actor._
import akka.event.LoggingReceive
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.`package`.DurationInt
import bank.Messages._

object Seller {
    val BID_TIME: FiniteDuration = 20 seconds
}


class Seller(val titles: List[String]) extends Actor {
  import Seller._
  var deletedAuctionsCount: Int = 0;
  var auctions: List[ActorRef] = List()
  def receive = LoggingReceive {
    case AuctionSold =>
    case AuctionDeleted => {
      deletedAuctionsCount += 1
      stopIfEnd()
    }
  }

  def createAuctions = {
    0 to titles.size-1 foreach { i => auctions = (context.actorOf(Props(classOf[Auction], titles(i), titles(i), BID_TIME, 0.0), "auction" + i) :: auctions) }
  }

  def stopIfEnd() = if (deletedAuctionsCount == auctions.size) context stop self

  createAuctions
}