package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor
import scala.concurrent.duration._
import scala.concurrent.duration.`package`.DurationInt
import bank.Messages._
import akka.persistence.PersistentActor
import akka.persistence.RecoveryCompleted
import java.util.Calendar
object Auction {
  val DELETE_TIME: FiniteDuration = 10 seconds

  case class BidEvent(prize: Double, winner: ActorPath)
  case object EndAuction
  case object DeleteAuction
  case class DeleteTime(time: FiniteDuration)
  case class EndTime(time: FiniteDuration)
  case class Created(initialPrice: Double)
}
class Auction(val title: String, val decription: String, val auctionDuration: FiniteDuration, val initialPrice: Double = 0.0) extends PersistentActor {
  import context._
  import Auction._

  def currentTime: FiniteDuration = Duration(Calendar.getInstance.getTimeInMillis, MILLISECONDS)

  def created(currentPrice: Double): Receive = LoggingReceive {
    case Bid(bidValue) if (bidValue > currentPrice) => {
      persist(BidEvent(bidValue, sender.path)) {
        evn =>
          context become activated(sender.path, bidValue) // TODO move this line to update state method
      }
    }
    case Bid(_) => sender ! BidTooLow(currentPrice)
    case BidTimerExpired => {
      persist(DeleteTime(currentTime + DELETE_TIME)) {
        evn => context.system.scheduler.scheduleOnce(DELETE_TIME, self, AuctionDeleted) // TODO move this line to update state method
      }
      context become ignored // TODO persist
    }

  }

  def activated(currentWinner: ActorPath, currentPrice: Double): Receive = LoggingReceive {
    case Bid(bidValue) if (bidValue > currentPrice) => {
      context.system.actorSelection(currentWinner) ! Outbidded(bidValue)
      persist(BidEvent(bidValue, sender.path)) {
        evn => context become activated(sender.path, bidValue)
      }
    }
    case Bid(_) if sender.path == currentWinner => println("Self-bid protection")
    case Bid(_)                                 => sender ! BidTooLow(currentPrice)
    case BidTimerExpired => {
      persist(EndAuction) {
        env =>
          {
            context.system.actorSelection(currentWinner) ! AuctionWon
            context.parent ! AuctionSold
            // TODO move this line to update state method
            context become sold
          }
      }
      persist(DeleteTime(currentTime + DELETE_TIME)) {
        evn => context.system.scheduler.scheduleOnce(DELETE_TIME, self, AuctionDeleted) // TODO move this line to update state method
      }
    }

  }

  def ignored: Receive = LoggingReceive {
    case Relist => {
      context become created(initialPrice) // TODO persist
      persist(EndTime(currentTime + auctionDuration)) {
        evn => context.system.scheduler.scheduleOnce(auctionDuration, self, BidTimerExpired) // TODO move this line to update state method
      }
    }
    case AuctionDeleted => {
      persist(DeleteAuction) {
        evn =>
          {
            context.parent ! AuctionDeleted
            // TODO move this line to update state method
            context.stop(self)
          }
      }
    }
  }

  def sold: Receive = LoggingReceive {
    case AuctionDeleted => {
      persist(DeleteAuction) {
        evn =>
          {
            context.parent ! AuctionDeleted
            // TODO move these lines to update state method
            context.actorSelection("/user/" + AuctionSearch.AUCTION_SEARCH_NAME) ! UnregisterAuction(title)
            context.stop(self)
          }
      }
    }
    case Bid(_) => sender ! AlreadySold
  }

  override def receiveRecover: Receive = LoggingReceive {
    case BidEvent(prize: Double, winner: ActorPath) => context become activated(winner, prize)
    case EndAuction                                 => context become sold
    case DeleteAuction =>
      context.actorSelection("/user/" + AuctionSearch.AUCTION_SEARCH_NAME) ! UnregisterAuction(title)
      context.stop(self)
    case DeleteTime(time: FiniteDuration) => if (time - currentTime > Duration(0, MILLISECONDS)) { context.system.scheduler.scheduleOnce(DELETE_TIME, self, AuctionDeleted) }
    case EndTime(time: FiniteDuration)    => if (time - currentTime > Duration(0, MILLISECONDS)) { context.system.scheduler.scheduleOnce(auctionDuration, self, BidTimerExpired) }
    case Created(initialPrice: Double)    => context become created(initialPrice)
    case RegisterAuction(title)           => context.actorSelection("/user/" + AuctionSearch.AUCTION_SEARCH_NAME) ! RegisterAuction(title)

    case RecoveryCompleted                => self ! Init // FIXME unhandled in all state expect default
  }

  override def persistenceId: String = "auction_" + title + decription + initialPrice + auctionDuration

  override def receiveCommand: Receive = LoggingReceive {
    case Init =>
      persist(RegisterAuction(title)) {
        evn => context.actorSelection("/user/" + AuctionSearch.AUCTION_SEARCH_NAME) ! RegisterAuction(title) // TODO move this line to update state method
      }
      persist(EndTime(currentTime + auctionDuration)) {
        evn => context.system.scheduler.scheduleOnce(auctionDuration, self, BidTimerExpired) // TODO move this line to update state method
      }
      persist(Created(initialPrice)) {
        evn => context become created(initialPrice) // TODO move this line to update state method
      }
  }

}
