package client.actor

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}

class Client(remote: InetSocketAddress) extends Actor with ActorLogging {

  import Tcp._
  import context.system
  IO(Tcp) ! Connect(remote)

  def receive: Receive = connecting

  def connecting: Receive = {
    case CommandFailed(_: Connect) =>
      log.error("Can't connect to server")
      context.stop(self)
      context.system.terminate()

    case Connected(remote, _) =>
      log.info(s"Connected to server $remote")
      val connection = sender()
      connection ! Register(self)
      context.become(processMessages(connection))
  }

  def processMessages(connection: ActorRef): Receive = {
    case Received(bytes) =>
      println(bytes.utf8String)
    case _: ConnectionClosed =>
      log.error("Connection closed")
      context.system.stop(self)
      context.system.terminate()
  }
}

object Client {
  def props(remote: InetSocketAddress) =
    Props(new Client(remote))
}
