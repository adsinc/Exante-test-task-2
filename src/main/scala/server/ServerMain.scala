package server

import java.net.InetSocketAddress
import java.time.LocalDateTime

import akka.actor.{ActorRef, ActorSystem}
import server.actor.Aggregator.NewMinuteStarted
import server.actor.{Aggregator, Server, UpstreamClient}

import scala.concurrent.duration.DurationDouble

object ServerMain extends App {
  val HistoryLen = 10
  val system = ActorSystem("server-system")
  val aggregator: ActorRef = system.actorOf(Aggregator.props(HistoryLen), "aggregator")
  system.actorOf(UpstreamClient.props(new InetSocketAddress("127.0.0.1", 5555), aggregator), "upstream-client")
  system.actorOf(Server.props(new InetSocketAddress("127.0.0.1", 7777), aggregator), "server")

  import system.dispatcher
  system.scheduler.schedule(1.minute - LocalDateTime.now().getSecond.seconds, 1.minute, aggregator, NewMinuteStarted)
}
