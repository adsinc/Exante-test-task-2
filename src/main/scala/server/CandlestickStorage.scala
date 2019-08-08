package server

import java.time.Instant
import java.time.temporal.ChronoUnit.MINUTES

import server.Data.Candlesticks.Candlestick
import server.Data.Transactions.Transaction
import server.Data.{Candlesticks, _}

import scala.collection.immutable.TreeMap
import scala.math.Ordering.Implicits._

class CandlestickStorage private(storage: TreeMap[Instant, TreeMap[Ticker, Candlestick]], historyLen: Int) {
  def updateFrom(msg: Transaction): CandlestickStorage = {
    val trunkedTs = msg.timestamp.truncatedTo(MINUTES)
    val tickerCandlesticks = storage.getOrElse(trunkedTs, TreeMap.empty[Ticker, Candlestick])
    val updatedCandleStick = tickerCandlesticks.get(msg.ticker)
      .map(Candlesticks.update(_, msg))
      .getOrElse(Candlesticks.create(trunkedTs, msg))
    val updatedStorage = storage + (trunkedTs -> (tickerCandlesticks + (msg.ticker -> updatedCandleStick)))
    new CandlestickStorage(updatedStorage, historyLen)
  }

  def tryRemoveOldCandlesticks(implicit currentTime: Instant): CandlestickStorage = {
    val minTimeStamp = minStoredTimestamp(currentTime)
    // todo тут ошибка
    // todo еще ошибка при очистке
    new CandlestickStorage(storage.dropWhile(_._1 <= minTimeStamp), historyLen)
  }

  private def minStoredTimestamp(currentTime: Instant): Instant =
    currentTime.truncatedTo(MINUTES).minus(historyLen + 1, MINUTES)

  def actualCandlesticks(implicit currentTime: Instant): Iterable[Candlestick] =
    for {
      allMinutesCandles <- storage.takeWhile(_._1 < currentTime).values
      candles <- allMinutesCandles.values
    } yield candles

  def prevMinuteCandlesticks(implicit currentTime: Instant): Iterable[Candlestick] =
    storage
      .get(currentTime.truncatedTo(MINUTES).minus(1, MINUTES))
      .map(_.values)
      .getOrElse(Iterable.empty)

}

object CandlestickStorage {
  def apply(historyLen: Int) = new CandlestickStorage(TreeMap.empty, historyLen)
}