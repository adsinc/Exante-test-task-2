package server.data

import java.time.Instant

import akka.util.ByteString
import server.data.Data.Transactions.Transaction

object Data {
  type Ticker = String

  object Transactions {
    final case class Transaction(timestamp: Instant, ticker: Ticker, price: Double, size: Int)

    // todo test
    // todo check for errors
    def convert(bytes: ByteString): Transaction = {
      val msgData = bytes.drop(2).toByteBuffer
      Transaction(
        timestamp = Instant.ofEpochMilli(msgData.getLong),
        ticker = (0 until msgData.getShort).map(_ => msgData.get().toChar).mkString,
        price = msgData.getDouble,
        size = msgData.getInt
      )
    }
  }

  object Candlesticks {
    final case class Candlestick(ticker: Ticker,
                                 timestamp: Instant,
                                 open: Double,
                                 high: Double,
                                 low: Double,
                                 close: Double,
                                 volume: Int)

    def update(candlestick: Candlestick, transaction: Transaction): Candlestick =
      candlestick.copy(
        high = math.max(transaction.price, candlestick.high),
        low = math.min(transaction.price, candlestick.low),
        close = transaction.price,
        volume = transaction.size
      )

    def create(timestamp: Instant, message: Transaction): Candlestick =
      Candlestick(
        ticker = message.ticker,
        timestamp = timestamp,
        open = message.price,
        high = message.price,
        low = message.price,
        close = message.price,
        volume = message.size
      )
  }
}
