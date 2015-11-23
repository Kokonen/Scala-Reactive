package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import bank.Messages._
import scala.concurrent.duration.`package`.DurationInt

class NotifierRequest extends Actor {

  implicit val timeout = Timeout(5 seconds)

  override def receive = LoggingReceive {
    case notifyMessage: Notify => {
      val auctionPublisher = Await.result(context.actorSelection("akka.tcp://AuctionPublisher@127.0.0.1:2553/user/AuctionPublisher").resolveOne()(5 seconds), 5 seconds)
      val futureResponse = auctionPublisher ? notifyMessage
      Await.result(futureResponse, timeout.duration)
      context stop self
    }
  }
}
