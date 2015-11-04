package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.`package`.DurationInt
import bank.Messages._

object AuctionFSM {

  sealed trait State
  case object Created extends State
  case object Activated extends State
  case object Ignored extends State
  case object Sold extends State

  sealed trait Data
  case class AuctionState(currentPrice: Double, currentWinner: ActorRef) extends Data
}

class AuctionFSM(val title: String, val decription: String, val auctionDuration: FiniteDuration, val initialPrice: Double = 0) extends FSM[AuctionFSM.State, AuctionFSM.Data] {
  import AuctionFSM._
  import context._
  
  val DELETE_TIME: FiniteDuration = 10 seconds

  when(Created) {
    case Event(Bid(bidValue), AuctionState(currentPrice, currentWinner)) if bidValue > currentPrice =>
      goto(Activated) using (AuctionState(bidValue, sender))
    case Event(Bid(bidValue), AuctionState(currentPrice, currentWinner)) => {
      sender ! BidTooLow(currentPrice)
      stay
    }
    case Event(BidTimerExpired, _) => {
      context.system.scheduler.scheduleOnce(DELETE_TIME, self, AuctionDeleted)
      goto(Ignored)
    }
  }

  when(Activated) {
    case Event(Bid(bidValue), AuctionState(currentPrice, currentWinner)) if bidValue > currentPrice => {
      currentWinner ! Outbidded(bidValue)
      stay using (AuctionState(bidValue, sender))
    }
    case Event(Bid(_), AuctionState(currentPrice, currentWinner)) => {
      sender ! BidTooLow(currentPrice)
      stay
    }
    case Event(BidTimerExpired, AuctionState(currentPrice, currentWinner)) => {
      currentWinner ! AuctionWon
      context.system.scheduler.scheduleOnce(DELETE_TIME, self, AuctionDeleted)
      goto(Sold)
    }
  }

  when(Ignored) {
    case Event(Relist, _) => {
      context.system.scheduler.scheduleOnce(auctionDuration, self, BidTimerExpired)
      goto(Created)
    }
    case Event(AuctionDeleted, _) => stop
  }

  when(Sold) {
    case Event(AuctionDeleted, _) => stop
    case Event(Bid(_),_) => {
      sender ! BidTimerExpired
      stay
    }
  }

  startWith(Created, AuctionState(initialPrice, null))
  context.system.scheduler.scheduleOnce(auctionDuration, self, BidTimerExpired)
}