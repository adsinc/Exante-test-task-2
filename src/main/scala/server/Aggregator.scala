package server

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.{Connected, PeerClosed, Write}
import akka.util.ByteString
import io.circe.generic.auto._
import io.circe.syntax._
import server.Aggregator.NewMinuteStarted
import server.Data.Candlesticks.Candlestick
import server.Data.Timestamps.{Timestamp, currentUtc}
import server.Data.Transactions.Transaction
import server.Server.NewClientConnected

class Aggregator(historyLen: Int) extends Actor with ActorLogging {
  implicit def currentTime: Timestamp = currentUtc()

  def receive: Receive = aggregate(CandlestickStorage(historyLen), Set.empty)

  def aggregate(storage: CandlestickStorage, clients: Set[ActorRef]): Receive = {
    case transaction: Transaction =>
      log.info(s"New transaction $transaction")
      context become aggregate(storage.updateFrom(transaction), clients)

    case NewClientConnected(client) =>
      log.info(s"Client connected: $client")
      sender() ! packToMessage(storage.actualCandlesticks)
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

  def props(historyLen: Int) = Props(new Aggregator(historyLen))
}