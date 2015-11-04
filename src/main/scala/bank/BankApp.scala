package bank
import akka.actor._
import akka.actor.Props
import scala.concurrent.duration.`package`.DurationInt

object BankApp extends App {
  val system = ActorSystem("BankApp")

  system.actorOf(Props(classOf[AuctionSearch]), AuctionSearch.AUCTION_SEARCH_NAME);

  var searchPhrases: List[String] = List("Audi", "manual", "silver")
  var auctionsTitles: List[String] = List("Audi A6 diesel manual", "Skoda fabia manual", "Daewoo tico silver", "Notebook asus silver")

  0 to 1 foreach { i => system.actorOf(Props(classOf[Seller], auctionsTitles.slice(2*i, 2*i+2)), "seller" + i) }
  
  Thread.sleep(500)

  0 to 4 foreach { i => system.actorOf(Props(classOf[Buyer], searchPhrases, 15.0), "buyer" + i) }
  system.awaitTermination()
}