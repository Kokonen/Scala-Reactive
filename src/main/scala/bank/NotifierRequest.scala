package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor
import bank.Messages._

class NotifierRequest(val message: Notify) extends Actor{
  def receive = LoggingReceive {
    case _ => 
  }
  
  context.actorSelection("akka.tcp://AuctionPublisher@127.0.0.1:2553/user/AuctionPublisher") ! message
  context stop self
}