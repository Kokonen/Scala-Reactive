package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.`package`.DurationInt
import bank.Messages._
object Auction {
  val DELETE_TIME: FiniteDuration = 10 seconds
}
class Auction(val title: String, val decription: String, val auctionDuration: FiniteDuration, val initialPrice: Double = 0.0) extends Actor {
  import context._
  import Auction._
  def created(currentPrice: Double): Receive = LoggingReceive {
    case Bid(bidValue) if (bidValue > currentPrice) => {
      context become activated(sender, bidValue)
    }
    case Bid(_) => sender ! BidTooLow(currentPrice)
    case BidTimerExpired => {
      context.system.scheduler.scheduleOnce(DELETE_TIME, self, AuctionDeleted)
      context become ignored
    }

  }

  def activated(currentWinner: ActorRef, currentPrice: Double): Receive = LoggingReceive {
    case Bid(bidValue) if (bidValue > currentPrice) => {
      currentWinner ! Outbidded(bidValue)
      context become activated(sender, bidValue)
    }
    case Bid(_) => sender ! BidTooLow(currentPrice)
    case BidTimerExpired => {
      currentWinner ! AuctionWon
      context.system.scheduler.scheduleOnce(DELETE_TIME, self, AuctionDeleted)

      context.parent ! AuctionSold

      context become sold
    }

  }

  def ignored: Receive = LoggingReceive {
    case Relist => {
      context.system.scheduler.scheduleOnce(auctionDuration, self, BidTimerExpired)
      context become created(initialPrice)
    }
    case AuctionDeleted => {
      context.parent ! AuctionDeleted
      context.stop(self)
    }
  }

  def sold: Receive = LoggingReceive {
    case AuctionDeleted => { 
      context.parent ! AuctionDeleted
      context.stop(self) }
    case Bid(_)         => sender ! AlreadySold
  }

  def receive = created(initialPrice)
  context.actorSelection("/user/" + AuctionSearch.AUCTION_SEARCH_NAME) ! RegisterAuction(title)
  context.system.scheduler.scheduleOnce(auctionDuration, self, BidTimerExpired)
}