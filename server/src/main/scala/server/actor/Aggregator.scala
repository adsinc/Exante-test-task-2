package server.actor

import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.{Connected, PeerClosed, Write}
import akka.util.ByteString
import io.circe.generic.auto._
import io.circe.syntax._
import server.actor.Aggregator.NewMinuteStarted
import server.actor.Server.NewClientConnected
import server.data.CandlestickStorage
import server.data.Data.Candlesticks.Candlestick
import server.data.Data.Transactions.Transaction

class Aggregator(currentTime: => Instant, historyLen: Int) extends Actor with ActorLogging {
  implicit def time: Instant = currentTime

  def receive: Receive = aggregate(CandlestickStorage(historyLen), Set.empty)

  def aggregate(storage: CandlestickStorage, clients: Set[ActorRef]): Receive = {
    case transaction: Transaction =>
      log.info(s"New transaction $transaction")
      context become aggregate(storage.updateFrom(transaction), clients)

    case NewClientConnected(client) =>
      log.info(s"Client connected: $client")
      client ! packToMessage(storage.actualCandlesticks)
      context become aggregate(storage, clients + client)

    case Connected(remoteAddress, _) =>
      log.info(s"Connected to upstream $remoteAddress")

    case PeerClosed =>
      log.info(s"Client disconnected ${sender()}")
      context become aggregate(storage, clients - sender())

    case NewMinuteStarted =>
      log.info("Try to clean up storage")
      val newStorage = storage.tryRemoveOldCandlesticks
      if (clients.nonEmpty) {
        log.info(s"Sending candlesticks to ${clients.size} clients")
        val msg = packToMessage(newStorage.prevMinuteCandlesticks)
        clients.foreach(_ ! msg)
      }
      context become aggregate(newStorage, clients)
  }

  private def packToMessage(candlesticks: Iterable[Candlestick]): Write =
    Write(
      ByteString(
        candlesticks.map(_.asJson.noSpaces).mkString("\n")
      )
    )
}

object Aggregator {
  case object NewMinuteStarted

  def props(currentTime: => Instant, historyLen: Int) = Props(new Aggregator(currentTime, historyLen))
}