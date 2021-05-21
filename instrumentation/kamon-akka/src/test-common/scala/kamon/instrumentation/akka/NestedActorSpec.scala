package kamon.instrumentation.akka

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import kamon.Kamon
import kamon.testkit.TestSpanReporter
import kamon.trace.Identifier
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class NestedActorSpec extends TestKit(ActorSystem("NestedActor")) with WordSpecLike with Matchers
  with BeforeAndAfterAll with ImplicitSender with Eventually with ScalaFutures with TestSpanReporter {

  "Child actors" should {
    "not inherit the context" in {
      val parent = system.actorOf(Props[ParentActor], "traced-probe-1")
      parent ! "gimme"
      val (parentSpanId, child) = expectMsgType[(Identifier, ActorRef)]
      child ! "gimme"
      val childSpanId = expectMsgType[Identifier]
      parentSpanId.isEmpty shouldBe false
      childSpanId.isEmpty shouldBe true
    }
  }
}

class ParentActor extends Actor {

  override def receive: Receive = {
    case _ =>
      val span = Kamon.currentSpan().id
      val child = context.actorOf(Props[ChildActor])
      sender ! (span, child)
  }
}

class ChildActor extends Actor {

  override def receive: Receive = {
    case _ => sender ! Kamon.currentSpan().id
  }
}
