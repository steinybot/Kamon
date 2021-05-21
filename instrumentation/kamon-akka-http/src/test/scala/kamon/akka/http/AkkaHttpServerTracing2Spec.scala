/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <https://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
*/

package kamon.akka.http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import kamon.Kamon
import kamon.testkit._
import kamon.trace.Span
import okhttp3.{OkHttpClient, Request, WebSocket, WebSocketListener}
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import java.util.concurrent.Executors
import javax.net.ssl.{HostnameVerifier, SSLSession}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Promise}

class AkkaHttpServerTracing2Spec extends WordSpecLike with Matchers with ScalaFutures with Inside with BeforeAndAfterAll
    with MetricInspection.Syntax with Reconfigure with TestWebServer with Eventually with OptionValues with TestSpanReporter {

  import TestWebServer.Endpoints._

  private val executionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))

  implicit private val system = ActorSystem("http-server-instrumentation-spec", None, None, Some(executionContext))
  implicit private val executor = system.dispatcher
  implicit private val materializer = ActorMaterializer()

  val (sslSocketFactory, trustManager) = clientSSL()
  val okHttp = new OkHttpClient.Builder()
    .sslSocketFactory(sslSocketFactory, trustManager)
    .hostnameVerifier(new HostnameVerifier { override def verify(s: String, sslSession: SSLSession): Boolean = true })
    .build()

  val timeoutTest: FiniteDuration = 5 second
  val interface = "127.0.0.1"
  val http1WebServer = startServer(interface, 8091, https = false)
  val http2WebServer = startServer(interface, 8092, https = true)

  testSuite("HTTP/1", http1WebServer)
  testSuite("HTTP/2", http2WebServer)

  def testSuite(httpVersion: String, server: WebServer) = {
    val interface = server.interface
    val port = server.port
    val protocol = server.protocol

    s"the Akka HTTP server instrumentation with ${httpVersion}" should {
      "cleanup the context" in {
        val target = s"$protocol://$interface:$port/$dummyPathOk"
        okHttp.newCall(new Request.Builder().url(target).build()).execute()

        val promise = Promise[Span]()
        executor.execute(() => {
          promise.success(Kamon.currentSpan())
        })
        val span = promise.future.futureValue
        span.isEmpty shouldBe true
      }
      "not interfere with any nested actor and stream" in {
        val target = s"$protocol://$interface:$port/$webSocket"
        val promise = Promise[String]
        val listener: WebSocketListener = new WebSocketListener {
          override def onMessage(webSocket: WebSocket, text: String): Unit = {
            promise.success(text)
            super.onMessage(webSocket, text)
          }
        }
        val ws = okHttp.newWebSocket(new Request.Builder().url(target).build(), listener)
        try {
          ws.send("Hello")
          val spanId = promise.future.futureValue(Timeout(5.seconds))
          spanId shouldBe Span.Empty.id.string
        } finally {
          ws.close(1000, null)
        }
      }
    }
  }

  override protected def afterAll(): Unit = {
    http1WebServer.shutdown()
    http2WebServer.shutdown()
  }
}

