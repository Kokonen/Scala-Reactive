package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor
import scala.concurrent.duration._
import scala.concurrent.duration.`package`.DurationInt
import bank.Messages._
import akka.persistence.PersistentActor
import akka.persistence.RecoveryCompleted
import akka.persistence.Recovery

import java.util.Calendar

object Auction {
  val DELETE_TIME: FiniteDuration = 10 seconds

  sealed trait BankEvent

  case class BidEvent(prize: Double, winner: ActorPath) extends BankEvent
  case object EndAuction extends BankEvent
  case object DeleteAuction extends BankEvent
  case class DeleteTime(time: FiniteDuration) extends BankEvent
  case class EndTime(time: FiniteDuration) extends BankEvent
  case class Created(initialPrice: Double) extends BankEvent
  case class Registered(title: String) extends BankEvent
  case object Ignored extends BankEvent
}
class Auction(val title: String, val decription: String, val auctionDuration: FiniteDuration, val initialPrice: Double = 0.0) extends PersistentActor {
  override val recovery = Recovery.none;
  import context._
  import Auction._

  def created(currentPrice: Double): Receive = LoggingReceive {
    case Bid(bidValue) if (bidValue > currentPrice) => {
      context.actorSelection("/user/" + Notifier.NOTIFIER_SEARCH_NAME) ! Notify(title, sender.path, bidValue)
      persist(BidEvent(bidValue, sender.path)) {
        evn =>
          updateState(evn)
      }
    }
    case Bid(_) => sender ! BidTooLow(currentPrice)
    case BidTimerExpired => {
      persist(DeleteTime(currentTime + DELETE_TIME)) {
        evn =>
          updateState(evn)
      }
      persist(Ignored) {
        evn =>
          updateState(evn)
      }
    }
  }

  def activated(currentWinner: ActorPath, currentPrice: Double): Receive = LoggingReceive {
    case Bid(bidValue) if (bidValue > currentPrice) => {
      context.system.actorSelection(currentWinner) ! Outbidded(bidValue)
      context.actorSelection("/user/" + Notifier.NOTIFIER_SEARCH_NAME) ! Notify(title, currentWinner, bidValue)
      persist(BidEvent(bidValue, sender.path)) {
        evn => updateState(evn)
      }
    }
    case Bid(_) if sender.path == currentWinner =>
    case Bid(_)                                 => sender ! BidTooLow(currentPrice)
    case BidTimerExpired => {
      persist(EndAuction) {
        evn =>
          updateState(evn)
      }
      context.system.actorSelection(currentWinner) ! AuctionWon
      context.parent ! AuctionSold
      persist(DeleteTime(currentTime + DELETE_TIME)) {
        evn => updateState(evn)
      }
    }
  }

  def ignored: Receive = LoggingReceive {
    case Relist => {
      persist(Created(initialPrice)) {
        evn => updateState(evn)
      }
      persist(EndTime(currentTime + auctionDuration)) {
        evn => updateState(evn)
      }
    }
    case AuctionDeleted => {
      persist(DeleteAuction) {
        evn =>
          updateState(evn)
      }
      context.parent ! AuctionDeleted
    }
  }

  def sold: Receive = LoggingReceive {
    case AuctionDeleted => {
      persist(DeleteAuction) {
        evn =>
          updateState(evn)
      }
      context.parent ! AuctionDeleted
    }
    case Bid(_) => sender ! AlreadySold
  }

  override def receiveRecover: Receive = LoggingReceive {
    case RecoveryCompleted => self ! Init
    case evn: BankEvent    => updateState(evn)
  }

  override def persistenceId: String = "auction_" + title + decription + initialPrice + auctionDuration

  override def receiveCommand: Receive = LoggingReceive {
    case Init =>
      persist(Registered(title)) {
        evn => updateState(evn); context.parent ! AuctionCreated
      }
      persist(EndTime(currentTime + auctionDuration)) {
        evn => updateState(evn)
      }
      persist(Created(initialPrice)) {
        evn => updateState(evn)
      }
  }

  def updateState(event: BankEvent) = {
    event match {
      case BidEvent(prize: Double, winner: ActorPath) => context become activated(winner, prize)
      case EndAuction                                 => context become sold
      case DeleteAuction =>
        context.actorSelection("/user/" + MasterSearch.MASTER_SEARCH_NAME) ! UnregisterAuction(title)
        context.stop(self)
      case DeleteTime(time: FiniteDuration) => {
        if (time - currentTime > Duration(0, MILLISECONDS)) {
          context.system.scheduler.scheduleOnce(DELETE_TIME, self, AuctionDeleted)
        } else {
          self ! AuctionDeleted
        }
      }
      case EndTime(time: FiniteDuration) => {
        if (time - currentTime > Duration(0, MILLISECONDS)) {
          context.system.scheduler.scheduleOnce(auctionDuration, self, BidTimerExpired)
        } else {
          self ! BidTimerExpired
        }
      }
      case Created(initialPrice: Double) => context become created(initialPrice)
      case Registered(title)             => context.actorSelection("/user/" + MasterSearch.MASTER_SEARCH_NAME) ! RegisterAuction(title)
      case Ignored                       => context become ignored
    }
  }

  def currentTime: FiniteDuration = Duration(Calendar.getInstance.getTimeInMillis, MILLISECONDS)

}
