package test

import akka.actor._
import akka.persistence._
import akka.event.LoggingReceive
import scala.concurrent.duration.`package`.DurationInt
import scala.concurrent.duration.`package`.DurationLong
import scala.util.Random
import bank.Messages._
import scala.concurrent.duration._
import java.util.Calendar

import akka.testkit.{ TestProbe, ImplicitSender, TestActorRef, TestKit }
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }

case object TIME
class SaveTestActor extends PersistentActor {
  override def recovery = Recovery.none // disable recovering

  var time: FiniteDuration = 0 millisecond;

  def currentTime: FiniteDuration = Duration(Calendar.getInstance.getTimeInMillis, MILLISECONDS)

  override def persistenceId: String = "readWriteTester"

  override def receiveCommand: Receive = {
    case TIME => {
      println("Write: " + time)
      val underTest2 = context.system.actorOf(Props[ReadTestActor], "readTestActor")
    }
    case x => {
      val start = currentTime
      persist(x) {
        _ => time += currentTime - start
      }
    }
  }
  override def receiveRecover: Receive = { // stub
    case _ =>
  }

  override def receive = receiveCommand
}

class ReadTestActor extends PersistentActor {
  override def recovery = Recovery(toSequenceNr = 1000L)

  var time: FiniteDuration = 0 millisecond;

  def currentTime: FiniteDuration = Duration(Calendar.getInstance.getTimeInMillis, MILLISECONDS)

  override def persistenceId: String = "readWriteTester"

  override def receiveCommand: Receive = {
    case _ =>
  }
  override def receiveRecover: Receive = { // stub
    case RecoveryCompleted => println("Read: " + (currentTime - start)); context.system.terminate();
  }

  val start = currentTime
}

object RedWriteTester extends App {
  val system = ActorSystem("BankApp")
  val underTest = system.actorOf(Props[SaveTestActor], "saveTestActor")

  1 to 1000 foreach { i => underTest ! Bid(i) }
  underTest ! TIME

}