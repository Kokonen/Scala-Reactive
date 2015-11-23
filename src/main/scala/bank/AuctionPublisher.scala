package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor

class AuctionPublisher extends Actor{
  def receive = LoggingReceive {
    case _ => 
  }
}