package bank

import akka.actor._

object Messages {

  case class Bid(bidValue: Double) {
    require(bidValue > 0)
  }

  case class BidTooLow(currentPrice: Double) {
    require(currentPrice > 0)
  }

  case class Outbidded(currentPrice: Double) {
    require(currentPrice > 0)
  }

  case class SearchResults(searchResults: List[ActorRef])

  case class Search(searchPhrase: String)

  case class RegisterAuction(title: String)
  case class UnregisterAuction(title: String)

  case class Notify(title: String, currentWinner: ActorPath, currentPrice: Double)

  case object AuctionSold
  case object BidTimerExpired
  case object AuctionDeleted
  case object Relist
  case object AuctionWon
  case object AlreadySold

  case object Init

  case object NotifyResponse
}
