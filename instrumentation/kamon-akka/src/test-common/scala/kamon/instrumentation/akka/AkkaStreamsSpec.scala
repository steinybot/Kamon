package kamon.instrumentation.akka

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.testkit.{ImplicitSender, TestKit}
import kamon.Kamon
import kamon.testkit.TestSpanReporter
import kamon.trace.Span
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future
import scala.concurrent.duration._

class AkkaStreamsSpec extends TestKit(ActorSystem("AkkaStreams")) with WordSpecLike with Matchers
  with BeforeAndAfterAll with ImplicitSender with Eventually with ScalaFutures with TestSpanReporter {

  "Akka-Streams created within an Actor" should {
    "not inherit the context" in {
      val traced = system.actorOf(Props[SinkProducerActor], "traced-probe-1")
      traced ! "gimme"
      val (actorSpan, sink) = expectMsgType[(Span, Sink[Any, Future[Span]])]
      val futureStreamSpan = Source.single("foo").toMat(sink)(Keep.right).run()
      val streamSpan = futureStreamSpan.futureValue
      actorSpan.isEmpty shouldBe false
      streamSpan.isEmpty shouldBe true
    }
  }
}

class SinkProducerActor extends Actor {

  override def receive: Receive = {
    case _ =>
      val span = Kamon.currentSpan()
      val sink: Sink[Any, Future[Span]] =
        Flow.fromFunction((_: Any) => Kamon.currentSpan())
          .toMat(Sink.head[Span])(Keep.right)
      sender ! (span, sink)
  }
}
