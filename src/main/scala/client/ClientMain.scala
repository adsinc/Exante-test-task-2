package client

import java.net.InetSocketAddress

import akka.actor.ActorSystem

object ClientMain extends App {
  val system: ActorSystem = ActorSystem("client-system")
  system.actorOf(Client.props(new InetSocketAddress("127.0.0.1", 7777)))
}