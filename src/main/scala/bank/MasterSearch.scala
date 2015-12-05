package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.routing._

import bank.Messages._

object MasterSearch {
  val MASTER_SEARCH_NAME = "MasterSearch"
}

class MasterSearch(val workersNumber: Int) extends Actor {

  val auctionSearches = Vector.fill(workersNumber) {
    val auctionSearch = context.actorOf(Props[AuctionSearch])
    context watch auctionSearch
    ActorRefRoutee(auctionSearch)
  }

  var registerRouter = {
    Router(BroadcastRoutingLogic(), auctionSearches)
  }

  var searchRouter = {
    Router(RoundRobinRoutingLogic(), auctionSearches)
  }

  def receive = LoggingReceive {
    case x: RegisterAuction   => registerRouter.route(x, sender)
    case x: UnregisterAuction => registerRouter.route(x, sender)
    case x: Search            => searchRouter.route(x, sender)
  }
}
