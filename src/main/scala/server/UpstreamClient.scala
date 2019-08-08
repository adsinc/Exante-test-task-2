package server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import server.Data.Transactions

class UpstreamClient(remote: InetSocketAddress, listener: ActorRef) extends Actor with ActorLogging {
  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive: Receive = connecting

  def connecting: Receive = {
    case CommandFailed(_: Connect) =>
      listener ! "connect failed"
      context.stop(self)

    case c@Connected(remote, local) =>
      listener ! c
      val connection = sender()
      connection ! Register(self)
      context.become(processMessages(connection))
  }

  def processMessages(connection: ActorRef): Receive = {
    case data: ByteString =>
      connection ! Write(data)
    case CommandFailed(w: Write) =>
      // O/S buffer was full
      listener ! "write failed"
    case Received(bytes) =>
      listener ! Transactions.convert(bytes)
    case "close" =>
      connection ! Close
    case _: ConnectionClosed =>
      // todo обработать ошибку соединения
      listener ! "connection closed"
      context.stop(self)
  }
}

object UpstreamClient {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(new UpstreamClient(remote, replies))
}