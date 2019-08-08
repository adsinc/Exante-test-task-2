package server.actor

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import server.actor.Server.NewClientConnected

class Server(address: InetSocketAddress, listener: ActorRef) extends Actor {

  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Bind(self, address)

  def receive: Receive = {
    case b@Bound(_) =>
      context.parent ! b

    case CommandFailed(_: Bind) => context stop self

    case Connected(_, _) =>
      val connection = sender()
      connection ! Register(listener)
      listener ! NewClientConnected(connection)
  }

}

object Server {
  final case class NewClientConnected(client: ActorRef)

  def props(address: InetSocketAddress, listener: ActorRef) =
    Props(new Server(address, listener))
}