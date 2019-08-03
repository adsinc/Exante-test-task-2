package server

import java.nio.ByteOrder
import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Sink, _}
import akka.util.ByteString
import server.Server.{Candlestick, CandlestickStorage, Message, Ticker, Timestamp, convert, createCandlestick}

import scala.concurrent._
import scala.concurrent.duration._

object Server {

  type Timestamp = Long
  type Ticker = String
  type CandlestickStorage = Map[Timestamp, Map[Ticker, Candlestick]]

  case class Message(timestamp: Timestamp,
                     ticker: Ticker,
                     price: Double,
                     size: Int)

  case class Candlestick(ticker: Ticker,
                         timestamp: Timestamp,
                         open: Double,
                         high: Double,
                         low: Double,
                         close: Double,
                         volume: Int)

  def updateFromMessage(candlestick: Candlestick, message: Message): Candlestick =
    candlestick.copy(
      high = math.max(message.price, candlestick.high),
      low = math.min(message.price, candlestick.low),
      close = message.price,
      volume = message.size
    )

  def createCandlestick(timestamp: Timestamp, message: Message): Candlestick =
    Candlestick(
      ticker = message.ticker,
      timestamp = timestamp,
      open = message.price,
      high = message.price,
      low = message.price,
      close = message.price,
      volume = message.size
    )

  // todo test
  // todo check for errors
  def convert(bytes: ByteString): Message = {
    val msgData = bytes.drop(2).toByteBuffer
    Message(
      timestamp = msgData.getLong,
      ticker = (0 until msgData.getShort).map(_ => msgData.get().toChar).mkString,
      price = msgData.getDouble,
      size = msgData.getInt
    )
  }
}

object UpstreamServerMain extends App {
  implicit val system: ActorSystem = ActorSystem("upstream-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val connection = Tcp().outgoingConnection("127.0.0.1", 5555)

  val convertInputData = Flow[ByteString]
    .via(Framing.lengthField(2, maximumFrameLength = 512, byteOrder = ByteOrder.BIG_ENDIAN))
    .map(convert)

  val logMessages = Flow[Message]
    .log("Message logger")
    .addAttributes(Attributes.logLevels(onElement = Attributes.LogLevels.Info))

  val msgFlow = Flow[ByteString]
    .via(connection)
    .via(convertInputData)

  val messages = Source.maybe[ByteString]
    .via(msgFlow)
    .scan(Map.empty[Timestamp, Map[Ticker, Candlestick]])(updateCandlestickStorage)


  def updateCandlestickStorage(storage: CandlestickStorage, msg: Message): CandlestickStorage = {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(msg.timestamp)
    val trunkedTs = TimeUnit.MINUTES.toMillis(minutes)
    val tickerCandlesticks = storage.getOrElse(trunkedTs, Map.empty[Ticker, Candlestick])
    val updatedCandleStick = tickerCandlesticks.get(msg.ticker)
        .map(Server.updateFromMessage(_, msg))
        .getOrElse(createCandlestick(trunkedTs, msg))
    storage + (trunkedTs -> (tickerCandlesticks + (msg.ticker -> updatedCandleStick)))
  }

  val ticks = Source.tick(0.second, 5.second, Instant.now())

  val merged = Source.combine(messages, ticks)(Merge(_))

  merged
    .runWith(Sink.foreach(p => println(s"tick received: [$p]")))
    .foreach(_ => system.terminate())

  // server
  val connections: Source[IncomingConnection, Future[ServerBinding]] =
    Tcp().bind("127.0.0.1", 7777)

  connections.runForeach { connection =>
    println(s"New connection from: ${connection.remoteAddress}")
    connection.flow.runWith(messages.map(r => ByteString(r.toString + "\n")), Sink.ignore)
  }
}
