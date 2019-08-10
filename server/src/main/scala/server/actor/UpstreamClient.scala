package server.actor

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import server.data.Data.Transactions

class UpstreamClient(remote: InetSocketAddress, listener: ActorRef) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive: Receive = connecting

  def connecting: Receive = {
    case CommandFailed(_: Connect) =>
      log.error("Can't connect to upstream server")
      context.stop(self)
      context.system.terminate()

    case c@Connected(_, _) =>
      log.info("Connected to upstream server")
      listener ! c
      val connection = sender()
      connection ! Register(self)
      context.become(processMessages(connection))
  }

  def processMessages(connection: ActorRef): Receive = {
    case data: ByteString =>
      connection ! Write(data)
    case Received(bytes) =>
      listener ! Transactions.convert(bytes)
    case _: ConnectionClosed =>
      log.error("Connection to upstream closed")
      context.stop(self)
      context.system.terminate()
  }
}

object UpstreamClient {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(new UpstreamClient(remote, replies))
}