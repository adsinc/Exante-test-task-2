package client

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString

class Client(remote: InetSocketAddress) extends Actor with ActorLogging {

  import Tcp._
  import context.system
  IO(Tcp) ! Connect(remote)

  def receive: Receive = connecting

  def connecting: Receive = {
    case CommandFailed(_: Connect) =>
      context.stop(self)

    case Connected(remote, _) =>
      log.info(s"Connected to server $remote")
      val connection = sender()
      connection ! Register(self)
      context.become(processMessages(connection))
  }

  def processMessages(connection: ActorRef): Receive = {
    case data: ByteString =>
      connection ! Write(data)
    case CommandFailed(w: Write) =>
    // O/S buffer was full
    //      listener ! "write failed"
    case Received(bytes) =>
      // todo
      println(bytes.utf8String)
    case "close" =>
      connection ! Close
    case _: ConnectionClosed =>
      // todo обработать ошибку соединения
//      listener ! "connection closed"
      context.stop(self)
  }
}

object Client {
  def props(remote: InetSocketAddress) =
    Props(new Client(remote))
}
