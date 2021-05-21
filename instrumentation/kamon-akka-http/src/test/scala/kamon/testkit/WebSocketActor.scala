package kamon.testkit

import akka.Done
import akka.actor._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Sink, Source}
import kamon.Kamon

import scala.concurrent.Future

object WebSocketActor {

  def props(): Props = Props(new WebSocketActor())
}

class WebSocketActor() extends Actor {

  override def receive: Receive = {
    case _: Any =>
      val source = Source.lazilyAsync(() => Future.successful(TextMessage(Kamon.currentSpan().id.string)))
      val sink: Sink[Message, Future[Done]] = Sink.ignore
      sender() ! (source, sink)
  }
}
