package server.data

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalatest.FunSuite
import server.data.Data.Candlesticks
import server.data.Data.Candlesticks.Candlestick
import server.data.Data.Transactions.Transaction

class DataTest extends FunSuite {
  val transactions: Seq[Transaction] = Seq(
    Transaction(Instant.parse("2016-01-01T15:02:10Z"), "AAPL", 101.1, 200),
    Transaction(Instant.parse("2016-01-01T15:02:15Z"), "AAPL", 101.2, 100),
    Transaction(Instant.parse("2016-01-01T15:02:25Z"), "AAPL", 101.3, 300),
    Transaction(Instant.parse("2016-01-01T15:02:40Z"), "AAPL", 101.0, 700)
  )

  val expectedCandle = Candlestick("AAPL", Instant.parse("2016-01-01T15:02:00Z"), 101.1, 101.3, 101, 101, 1300)

  test("update candlestick") {
    val candle = Candlesticks.create(
      transactions.head.timestamp.truncatedTo(ChronoUnit.MINUTES),
      transactions.head
    )
    val resultCandle = transactions.tail.foldLeft(candle)(Candlesticks.update)
    assert(resultCandle == expectedCandle)
  }
}
