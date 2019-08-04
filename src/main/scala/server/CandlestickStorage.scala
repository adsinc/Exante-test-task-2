package server

import server.Data.Candlesticks.Candlestick
import server.Data.Timestamps.Timestamp
import server.Data.Transactions.Transaction
import server.Data.{Candlesticks, _}

import scala.collection.immutable.TreeMap

class CandlestickStorage private(storage: TreeMap[Timestamp, Map[Ticker, Candlestick]], historyLen: Int) {
  def updateFrom(msg: Transaction): CandlestickStorage = {
    val trunkedTs = Timestamps.trunkToMinutes(msg.timestamp)
    val tickerCandlesticks = storage.getOrElse(trunkedTs, Map.empty[Ticker, Candlestick])
    val updatedCandleStick = tickerCandlesticks.get(msg.ticker)
      .map(Candlesticks.update(_, msg))
      .getOrElse(Candlesticks.create(trunkedTs, msg))
    val updatedStorage = storage + (trunkedTs -> (tickerCandlesticks + (msg.ticker -> updatedCandleStick)))
    new CandlestickStorage(updatedStorage, historyLen)
  }

  def tryRemoveOldCandlesticks(implicit currentTime: Timestamp): CandlestickStorage = {
    val minTimeStamp = Timestamps.minStoredTimestamp(currentTime, historyLen)
    new CandlestickStorage(storage.takeWhile(_._1 >= minTimeStamp), historyLen)
  }

  def actualCandlesticks(implicit currentTime: Timestamp): Iterable[Candlestick] =
    for {
      allMinutesCandles <- storage.takeWhile(_._1 < currentTime).values
      candles <- allMinutesCandles.values
    } yield candles

  def prevMinuteCandlesticks(implicit currentTime: Timestamp): Iterable[Candlestick] = ???


}

object CandlestickStorage {
  def apply(historyLen: Int) = new CandlestickStorage(TreeMap.empty, historyLen)
}