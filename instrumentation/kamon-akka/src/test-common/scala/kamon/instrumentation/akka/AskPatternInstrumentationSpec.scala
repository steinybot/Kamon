/*
 * =========================================================================================
 * Copyright © 2013 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.instrumentation.akka


import akka.actor._
import akka.pattern.ask
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpecLike}
import ContextTesting._
import kamon.testkit.TestSpanReporter
import kamon.trace.Span
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scala.concurrent.duration._

class AskPatternInstrumentationSpec extends TestKit(ActorSystem("AskPatternInstrumentationSpec")) with WordSpecLike with BeforeAndAfterAll
  with ImplicitSender with Matchers with ScalaFutures with Eventually with OptionValues with TestSpanReporter {

  implicit lazy val ec = system.dispatcher
  implicit val askTimeout = Timeout(10 millis)

  // TODO: Make this work with ActorSelections

  "the AskPatternInstrumentation" when {
    "configured in heavyweight mode" should {
      "log a warning with a full stack trace and the context captured the moment when the ask was triggered for an actor" in {
        val noReplyActorRef = system.actorOf(Props[NoReply], "no-reply-1")
        setAskPatternTimeoutWarningMode("heavyweight")

        EventFilter.warning(start = "Timeout triggered for ask pattern to actor [no-reply-1] at").intercept {
          Kamon.runWithContext(testContext("ask-timeout-warning")) {
            noReplyActorRef ? "hello"
          }
        }
      }
    }

    "configured in lightweight mode" should {
      "log a warning with a short source location description and the context taken from the moment the ask was triggered for a actor" in {
        val noReplyActorRef = system.actorOf(Props[NoReply], "no-reply-2")
        setAskPatternTimeoutWarningMode("lightweight")

        EventFilter.warning(start = "Timeout triggered for ask pattern to actor [no-reply-2] at").intercept {
          Kamon.runWithContext(testContext("ask-timeout-warning")) {
            noReplyActorRef ? "hello"
          }
        }
      }
    }

    "configured in off mode" should {
      "should not log any warning messages" in {
        val noReplyActorRef = system.actorOf(Props[NoReply], "no-reply-3")
        setAskPatternTimeoutWarningMode("off")

        intercept[AssertionError] { // No message will be logged and the event filter will fail.
          EventFilter.warning(start = "Timeout triggered for ask pattern to actor", occurrences = 1).intercept {
            Kamon.runWithContext(testContext("ask-timeout-warning")) {
              noReplyActorRef ? "hello"
            }
          }
        }
      }
    }
  }

  it should {
    "create subsequent spans as siblings of the ask span" in {
      val first = system.actorOf(Props[EchoActor], "traced-first")
      val second = system.actorOf(Props[EchoActor], "traced-second")

      val span = Kamon.spanBuilder("test").start()
      Kamon.runWithSpan(span) {
        val future = for {
          _ <- first ? "Hello?"
          _ <- second ? "Goodbye!"
        } yield ()

        future.isReadyWithin(2 seconds)

        def nextSpan = {
          eventually(timeout(20 seconds)) {
            testSpanReporter.nextSpan().value
          }
        }

        nextSpan.parentId shouldBe span.id
        nextSpan.parentId shouldBe span.id
      }
    }
  }

  override protected def beforeAll(): Unit = {
    enableFastSpanFlushing()
    sampleAlways()
  }

  override protected def afterAll(): Unit = shutdown()

  def setAskPatternTimeoutWarningMode(mode: String): Unit = {
    val newConfiguration = ConfigFactory.parseString(s"kamon.akka.ask-pattern-timeout-warning=$mode").withFallback(Kamon.config())
    Kamon.reconfigure(newConfiguration)
  }
}

class NoReply extends Actor {
  def receive = {
    case _ =>
  }
}

class EchoActor extends Actor {

  override def receive: Receive = {
    case message => sender ! s"Echo: ${message.toString}"
  }
}
