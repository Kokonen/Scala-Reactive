package test

import akka.actor._
import akka.event.LoggingReceive
import scala.concurrent.duration.`package`.DurationInt
import com.typesafe.config._
import scala.concurrent.Await
import scala.concurrent.duration._
import java.util.Calendar
import bank._
import bank.Messages._
import java.util.stream.IntStream
import java.util.stream.Collectors

class BuyerStub extends Actor {
  var time: FiniteDuration = 0 millisecond;

  var receiveMsg = 0;

  def receive = LoggingReceive {
    case SearchResults(searchResults) => {
      receiveMsg += 1
      if (receiveMsg == RouterTest.searchs) {
        println(RouterTest.currentTime - time)
      }
    }
  }

  println("Start searching")
  time = RouterTest.currentTime
  0 to RouterTest.searchs foreach { name => context.actorSelection("/user/MasterSearch") ! Search(name.toString()) }
}

class RouterTestActor extends Actor {
  def receive = LoggingReceive {
    case AuctionsCreated => context.actorOf(Props[BuyerStub], "buyer")
  }

  context.actorOf(Props(classOf[Seller], (0 to RouterTest.auctions).toList.map { x => x.toString() }), "seller")
}

object RouterTest extends App {
  val searchWorkersCount = 10
  val auctions = 5000
  val searchs = 2000
  def currentTime: FiniteDuration = Duration(Calendar.getInstance.getTimeInMillis, MILLISECONDS)

  val config = ConfigFactory.load()
  val system = ActorSystem("BankApp", config.getConfig("BankApp").withFallback(config))

  system.actorOf(Props(classOf[MasterSearch], searchWorkersCount), MasterSearch.MASTER_SEARCH_NAME);
  system.actorOf(Props(classOf[Notifier]), Notifier.NOTIFIER_SEARCH_NAME);

  system.actorOf(Props[RouterTestActor])
}
