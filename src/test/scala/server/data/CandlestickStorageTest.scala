package server.data

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalatest.FunSuite
import server.data.Data.Transactions.Transaction

import scala.concurrent.duration._

class CandlestickStorageTest extends FunSuite {
  private val currentTime = Instant.parse("2016-01-01T15:02:13Z")

  private def createTransaction(timeDiff: FiniteDuration) =
    Transaction(
      currentTime.minusSeconds(timeDiff.toSeconds),
      "TEST",
      1.0,
      1
    )

  test("testPrevMinuteCandlesticks") {
    val capacity = 3
    val storage = (0 until capacity)
      .map(diff => createTransaction(diff.minutes))
      .foldLeft(CandlestickStorage(capacity))(_ updateFrom _)
    assume(storage.storage.size == 3)
    for {
      diff <- 0 until capacity - 1
      time = currentTime.minusSeconds(diff.minutes.toSeconds)
    } {
      val candles = storage.prevMinuteCandlesticks(time)
      assert(candles.size == 1)
      assert(candles.head.timestamp == time.truncatedTo(ChronoUnit.MINUTES).minus(1, ChronoUnit.MINUTES))
    }
  }

  test("testActualCandlesticks") {

  }

  test("testUpdateFrom") {

  }

  test("testTryRemoveOldCandlesticks") {

  }

  test("testApply") {

  }

}
