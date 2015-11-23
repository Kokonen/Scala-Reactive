package bank

import akka.actor._
import akka.event.LoggingReceive
import akka.actor.Actor
import bank.Messages._
import akka.actor.SupervisorStrategy.{ Restart }
import scala.concurrent.duration.`package`.DurationInt

object Notifier {
  val NOTIFIER_SEARCH_NAME = "Notifier"
}

class Notifier extends Actor{
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1 minute) {
    case e => {
      e.printStackTrace(); 
      Restart;
    }
  }
  
  
  def receive = LoggingReceive {
    case notify: Notify => {
      context.actorOf(Props(classOf[NotifierRequest], notify))     
    }
  } 
}