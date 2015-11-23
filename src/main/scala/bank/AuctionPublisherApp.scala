package bank

import com.typesafe.config._
import akka.actor._
 

object BankPublisherApp extends App{
    val config = ConfigFactory.load()
    val auctionPublisherSystem = ActorSystem("AuctionPublisher", config.getConfig("AuctionPublisher").withFallback(config))
    val auctionPublisher = auctionPublisherSystem.actorOf(Props[AuctionPublisher], "AuctionPublisher")
}