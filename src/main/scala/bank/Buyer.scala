package bank

import akka.actor._
import akka.event.LoggingReceive
import bank.Messages._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.`package`.DurationInt
import scala.util.Random

class Buyer(var searchTitles: List[String], val maximumBid: Double) extends Actor {
  import context._
  var endedAuctionsCount: Int = 0
  var auctions: List[ActorRef] = List()

  def receive = LoggingReceive {
    case AuctionWon =>
      endedAuctionsCount += 1
      stopIfEnd()
    case AlreadySold =>
      endedAuctionsCount += 1
      stopIfEnd()
    case SearchResults(searchResults) => {
      searchResults.foreach {
        auction =>
          {
            if(!auctions.contains(auction)){
              auctions = auction::auctions
              auction ! Bid(10)
            }
          }
      }
    }

    case Outbidded(currentPrice) => scheduleNextBid(sender, currentPrice)
    case BidTooLow(currentPrice) => scheduleNextBid(sender, currentPrice)
  }

  def stopIfEnd() = if (endedAuctionsCount >= auctions.size) {
    context stop self
  }

  def scheduleNextBid(auction: ActorRef, currentPrice: Double) = {
    var potentialBid: Double = currentPrice + 1
    if (maximumBid > currentPrice) {
      if (potentialBid > maximumBid) {
        potentialBid = maximumBid
      }
      context.system.scheduler.scheduleOnce(Random.nextInt(1500) + 250 milliseconds, auction, Bid(potentialBid))
    } else {
      endedAuctionsCount += 1
    }
    stopIfEnd()
  }

  def startBidding = {
    var auctionSearch: ActorSelection = context.actorSelection("/user/"+ AuctionSearch.AUCTION_SEARCH_NAME)
    searchTitles.foreach { title => auctionSearch ! Search(title) }
  }

  startBidding
}