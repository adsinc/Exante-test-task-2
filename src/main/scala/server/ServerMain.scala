package server

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem}
import server.Aggregator.NewMinuteStarted

import scala.concurrent.duration.DurationDouble

object ServerMain extends App {
  val system = ActorSystem("server-system")
  val aggregator: ActorRef = system.actorOf(Aggregator.props)
  system.actorOf(UpstreamClient.props(new InetSocketAddress("127.0.0.1", 5555), aggregator))
  system.actorOf(Server.props(new InetSocketAddress("127.0.0.1", 7777), aggregator))

  import system.dispatcher
  // todo return to normal
//  system.scheduler.schedule(1.minute - LocalDateTime.now().getSecond.seconds, 1.minute, aggregator, NewMinuteStarted)
  system.scheduler.schedule(1.seconds, 5.seconds, aggregator, NewMinuteStarted)
}
