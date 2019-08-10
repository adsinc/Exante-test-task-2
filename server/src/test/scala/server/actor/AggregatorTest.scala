package server.actor

import java.time.Instant

import akka.actor.ActorSystem
import akka.io.Tcp.Write
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import io.circe.generic.auto._
import io.circe.parser.decode
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}
import server.actor.Aggregator.NewMinuteStarted
import server.actor.Server.NewClientConnected
import server.data.Data.Candlesticks.Candlestick
import server.data.Data.Transactions.Transaction


class AggregatorTest extends TestKit(ActorSystem("AggregatorTest"))
  with ImplicitSender
  with FunSuiteLike
  with BeforeAndAfterAll {

  val transactions: Seq[Transaction] = Seq(
    Transaction(Instant.parse("2016-01-01T15:02:10Z"), "AAPL", 101.1, 200),
    Transaction(Instant.parse("2016-01-01T15:02:15Z"), "AAPL", 101.2, 100),
    Transaction(Instant.parse("2016-01-01T15:02:25Z"), "AAPL", 101.3, 300),
    Transaction(Instant.parse("2016-01-01T15:02:35Z"), "MSFT", 120.1, 500),
    Transaction(Instant.parse("2016-01-01T15:02:40Z"), "AAPL", 101.0, 700),
    Transaction(Instant.parse("2016-01-01T15:03:10Z"), "AAPL", 102.1, 1000),
    Transaction(Instant.parse("2016-01-01T15:03:11Z"), "MSFT", 120.2, 1000),
    Transaction(Instant.parse("2016-01-01T15:03:30Z"), "AAPL", 103.2, 100),
    Transaction(Instant.parse("2016-01-01T15:03:31Z"), "MSFT", 120.0, 700),
    Transaction(Instant.parse("2016-01-01T15:04:21Z"), "AAPL", 102.1, 100),
    Transaction(Instant.parse("2016-01-01T15:04:21Z"), "MSFT", 102.1, 200),
  )

  val expectedCandleOnConnect: Array[Candlestick] = Array(
    Candlestick("AAPL", Instant.parse("2016-01-01T15:02:00Z"), 101.1, 101.3, 101, 101, 1300),
    Candlestick("MSFT", Instant.parse("2016-01-01T15:02:00Z"), 120.1, 120.1, 120.1, 120.1, 500),
    Candlestick("AAPL", Instant.parse("2016-01-01T15:03:00Z"), 102.1, 103.2, 102.1, 103.2, 1100),
    Candlestick("MSFT", Instant.parse("2016-01-01T15:03:00Z"), 120.2, 120.2, 120, 120, 1700),
  )

  val expectedOnNewMinute: Array[Candlestick] = Array(
    Candlestick("AAPL", Instant.parse("2016-01-01T15:04:00Z"), 102.1, 102.1, 102.1, 102.1, 100),
    Candlestick("MSFT", Instant.parse("2016-01-01T15:04:00Z"), 102.1, 102.1, 102.1, 102.1, 200),
  )

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  test("Aggregating transactions. New client receives previous candles on connect and every NewMinuteStarted") {
    var time = Instant.parse("2016-01-01T15:04:04Z")
    def getTime: Instant = time
    val aggregator = system.actorOf(Aggregator.props(getTime, 3))
    // add transactions
    transactions.foreach(aggregator ! _)
    val client = TestProbe()
    aggregator ! NewClientConnected(client.ref)

    def parseDada(data: ByteString): Array[Candlestick] =
      data.utf8String
        .split("\n")
        .flatMap(decode[Candlestick](_).toSeq)

    client.fishForSpecificMessage(1.seconds) {
      case Write(data, _) =>
        assert(parseDada(data) sameElements expectedCandleOnConnect)
    }
    // change time
    time = Instant.parse("2016-01-01T15:05:00Z")
    aggregator ! NewMinuteStarted
    client.fishForSpecificMessage(1.seconds) {
      case Write(data, _) =>
        assert(parseDada(data) === expectedOnNewMinute)
    }
  }
}
