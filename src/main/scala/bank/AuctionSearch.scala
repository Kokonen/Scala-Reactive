package bank

import akka.actor._
import akka.event.LoggingReceive
import bank.Messages._

object AuctionSearch {
  val AUCTION_SEARCH_NAME = "AuctionSearch"
}

class AuctionSearch extends Actor {

  def receive(searchMap: Map[String, ActorRef]): Receive = LoggingReceive {
    case Search(searchPhrase) => sender ! SearchResults(search(searchPhrase, searchMap))

    case RegisterAuction(auctionTitle) => {
      context become receive(searchMap + (auctionTitle -> sender));
    }
  }

  def receive = receive(Map())

  def search(searchPhrase: String, searchMap: Map[String, ActorRef]): List[ActorRef] = {
    searchMap.filterKeys { title => title.contains(searchPhrase) }.values.toList
  }
}