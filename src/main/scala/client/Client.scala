package client

import java.nio.ByteOrder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Framing, Sink, Source, Tcp}
import akka.util.ByteString

import scala.concurrent.ExecutionContext

object Client {

}

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("upstream-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val connection = Tcp().outgoingConnection("127.0.0.1", 7777)

  val convertInputData = Flow[ByteString]
    .via(Framing.delimiter(ByteString("\n"), 256, allowTruncation = true))
    .map(_.utf8String)

  val messages = Source.maybe[ByteString]
    .via(connection)
    .via(convertInputData)

  messages.runWith(Sink.foreach(println))
    .foreach(_ => system.terminate())
}
