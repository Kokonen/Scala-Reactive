package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor
import bank.Messages._
import akka.actor.SupervisorStrategy.{ Restart, Stop }
import scala.concurrent.duration.`package`.DurationInt
import java.util.concurrent.TimeoutException

object Notifier {
  val NOTIFIER_SEARCH_NAME = "Notifier"
}

class Notifier extends Actor {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1 minute) {
    case _: ActorNotFound => {
      Restart;
    }
    case _: TimeoutException => Restart // Nie dostał odpowiedzi
    case _: Exception        => Stop // inny wyjątek
  }

  override def receive = LoggingReceive {
    case notify: Notify => {
      context.actorOf(Props[NotifierRequest]) ! notify
    }
  }
}
