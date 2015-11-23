package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor
import bank.Messages.NotifyResponse

class AuctionPublisher extends Actor {
  override def receive = LoggingReceive {
    case _ => sender ! NotifyResponse
  }
}
