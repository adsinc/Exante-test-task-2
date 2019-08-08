package server.data

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalatest.FunSuite
import server.data.Data.Candlesticks.Candlestick
import server.data.Data.Transactions.Transaction

import scala.concurrent.duration._

class CandlestickStorageTest extends FunSuite {
  private val currentTime = Instant.parse("2016-01-01T15:02:13Z")
  private val baseTestTransaction = Transaction(currentTime, "TEST", 1.0, 1)

  private def createTransaction(timeDiff: FiniteDuration): Transaction =
    baseTestTransaction.copy(timestamp = currentTime.minusSeconds(timeDiff.toSeconds))

  private def createStorage(historyLen: Int): CandlestickStorage = {
    val storage = (0 to historyLen)
      .map(diff => createTransaction(diff.minutes))
      .foldLeft(CandlestickStorage(historyLen))(_ updateFrom _)
    assume(storage.storage.size == historyLen + 1)
    storage
  }

  private def prevMinutesWithDiffs(count: Int): Seq[(Instant, Int)] =
    for {
      diff <- 0 until count
      time = currentTime.minusSeconds(diff.minutes.toSeconds)
    } yield (time, diff)

  test("testPrevMinuteCandlesticks") {
    val capacity = 3
    val storage = createStorage(capacity)
    prevMinutesWithDiffs(capacity - 1)
      .map(_._1)
      .foreach { time =>
        val candles = storage.prevMinuteCandlesticks(time)
        assert(candles.size == 1)
        assert(candles.head.timestamp == time.truncatedTo(ChronoUnit.MINUTES).minus(1, ChronoUnit.MINUTES))
      }
  }

  test("testActualCandlesticks") {
    val capacity = 5
    val storage = createStorage(capacity)
    prevMinutesWithDiffs(capacity - 1).foreach {
      case (time, diff) =>
        assert(storage.actualCandlesticks(time).size == capacity - diff)
    }
  }

  test("testUpdateFrom") {
    val capacity = 5
    val storage = createStorage(5)
    prevMinutesWithDiffs(capacity - 1).foreach {
      case (t, _) =>
        val timeMinute = t.truncatedTo(ChronoUnit.MINUTES)
        val transaction = baseTestTransaction.copy(timeMinute)
        val newStorage = storage.updateFrom(transaction)
        for {
          minuteCandles <- newStorage.storage.get(timeMinute)
          candle <- minuteCandles.get("TEST")
        } assert(candle == Candlestick("TEST", timeMinute, 1.0, 1.0, 1.0, 1.0, 2))
    }
  }

  test("testTryRemoveOldCandlesticks") {
    val capacity = 3
    val storage = createStorage(capacity)
    val time = currentTime.plus(capacity, ChronoUnit.MINUTES)
    val newStorage = storage.tryRemoveOldCandlesticks(time)
    assert(newStorage.actualCandlesticks(time).size == 1)
  }

}
