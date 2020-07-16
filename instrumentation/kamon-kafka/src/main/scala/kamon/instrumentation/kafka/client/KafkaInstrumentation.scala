package kamon.instrumentation.kafka.client

import com.typesafe.config.Config
import kamon.Kamon
import kamon.context.Context
import kamon.instrumentation.context.HasContext
import kamon.trace.Span
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.util.control.NonFatal

object KafkaInstrumentation {
  @volatile private var _settings: Settings = readSettings(Kamon.config())
  Kamon.onReconfigure((newConfig: Config) => _settings = readSettings(newConfig))

  def settings: Settings =
    _settings

  private def readSettings(config: Config): Settings = {
    val kafkaConfig = config.getConfig("kamon.instrumentation.kafka.client")

    Settings(
      continueTraceOnConsumer = kafkaConfig.getBoolean("tracing.continue-trace-on-consumer"),
      useDelayedSpans = kafkaConfig.getBoolean("tracing.use-delayed-spans")
    )
  }

  def extractContext[K, V](consumerRecord: ConsumerRecord[K, V]): Context = {
    consumerRecord.context
  }

  /**
    * Syntactical sugar to extract a Context from a ConsumerRecord instance.
    */
  implicit class Syntax(val cr: ConsumerRecord[_, _]) extends AnyVal{
    def context: Context = cr match {
      case hc: HasContext => hc.context
      case _              => Context.Empty
    }
  }

  /**
    * Creates a new consumer Span for the provided consumer record and sets it as the current Span while running the
    * provided block of code. This function can optionally finish the Span once the code block execution has finished.
    *
    * The relationship between the trace on the producer side of the topic and the new consumer Span is controlled by
    * the "continue-trace-on-consumer" setting:
    *   - When enabled (default), the new consumer Span will be created as a child Span of the producer's Span. Both
    *     Spans will be part of the same trace.
    *   - When disabled, the new consumer Span will start a new trace, but will have a link to the Span on the producer
    *     side.
    *
    * NOTE: Continuing or linking Spans to the trace on the producer side is only possible when automatic
    *       instrumentation is enabled. If you are running your application without Kanela then you will only get a
    *       simple consumer Span, without any connection to the original trace.
    */
  def runWithConsumerSpan[T](record: ConsumerRecord[_, _])(f: => T): T =
    runWithConsumerSpan(record, "consumer.process", true)(f)

  /**
    * Creates a new consumer Span for the provided consumer record and sets it as the current Span while running the
    * provided block of code. This function can optionally finish the Span once the code block execution has finished.
    *
    * The relationship between the trace on the producer side of the topic and the new consumer Span is controlled by
    * the "continue-trace-on-consumer" setting:
    *   - When enabled (default), the new consumer Span will be created as a child Span of the producer's Span. Both
    *     Spans will be part of the same trace.
    *   - When disabled, the new consumer Span will start a new trace, but will have a link to the Span on the producer
    *     side.
    *
    * NOTE: Continuing or linking Spans to the trace on the producer side is only possible when automatic
    *       instrumentation is enabled. If you are running your application without Kanela then you will only get a
    *       simple consumer Span, without any connection to the original trace.
    */
  def runWithConsumerSpan[T](record: ConsumerRecord[_, _], operationName: String)(f: => T): T =
    runWithConsumerSpan(record, operationName, true)(f)

  /**
    * Creates a new consumer Span for the provided consumer record and sets it as the current Span while running the
    * provided block of code. This function can optionally finish the Span once the code block execution has finished.
    *
    * The relationship between the trace on the producer side of the topic and the new consumer Span is controlled by
    * the "continue-trace-on-consumer" setting:
    *   - When enabled (default), the new consumer Span will be created as a child Span of the producer's Span. Both
    *     Spans will be part of the same trace.
    *   - When disabled, the new consumer Span will start a new trace, but will have a link to the Span on the producer
    *     side.
    *
    * NOTE: Continuing or linking Spans to the trace on the producer side is only possible when automatic
    *       instrumentation is enabled. If you are running your application without Kanela then you will only get a
    *       simple consumer Span, without any connection to the original trace.
    */
  def runWithConsumerSpan[T](record: ConsumerRecord[_, _], operationName: String, finishSpan: Boolean)(f: => T): T = {
    val incomingContext = record.context
    val operationContext = if(incomingContext.nonEmpty()) incomingContext else Kamon.currentContext()
    val span = consumerSpan(record, operationName)
    val scope = Kamon.storeContext(operationContext.withEntry(Span.Key, span))

    try {
      f
    } catch {
      case NonFatal(t) =>
        span.fail(t.getMessage, t)
        throw t

    } finally {
      if(finishSpan)
        span.finish()

      scope.close()
    }
  }

  /**
    * Creates a new consumer Span for the provided consumer record. The relationship between the trace on the producer
    * side of the topic and the new consumer Span is controlled by the "continue-trace-on-consumer" setting:
    *   - When enabled (default), the new consumer Span will be created as a child Span of the producer's Span. Both
    *     Spans will be part of the same trace.
    *   - When disabled, the new consumer Span will start a new trace, but will have a link to the Span on the producer
    *     side.
    *
    * NOTE: Continuing or linking Spans to the trace on the producer side is only possible when automatic
    *       instrumentation is enabled. If you are running your application without Kanela then you will only get a
    *       simple consumer Span, without any connection to the original trace.
    */
  def consumerSpan(record: ConsumerRecord[_, _]): Span =
    consumerSpan(record, "consumer.process")

  /**
    * Creates a new consumer Span for the provided consumer record. The relationship between the trace on the producer
    * side of the topic and the new consumer Span is controlled by the "continue-trace-on-consumer" setting:
    *   - When enabled (default), the new consumer Span will be created as a child Span of the producer's Span. Both
    *     Spans will be part of the same trace.
    *   - When disabled, the new consumer Span will start a new trace, but will have a link to the Span on the producer
    *     side.
    *
    * NOTE: Continuing or linking Spans to the trace on the producer side is only possible when automatic
    *       instrumentation is enabled. If you are running your application without Kanela then you will only get a
    *       simple consumer Span, without any connection to the original trace.
    */
  def consumerSpan(record: ConsumerRecord[_, _], operationName: String): Span = {
    val consumerSpan = Kamon.consumerSpanBuilder(operationName, "kafka.consumer")
      .tag("kafka.topic", record.topic())
      .tag("kafka.partition", record.partition())
      .tag("kafka.offset", record.offset)
      .tag("kafka.timestamp", record.timestamp())
      .tag("kafka.timestampType", record.timestampType.name)

    Option(record.key()).foreach(k => consumerSpan.tag("kafka.key", k.toString()))

    // The additional context information will only be available when instrumentation is enabled.
    if(record.isInstanceOf[ConsumedRecordData]) {
      val consumerRecordData = record.asInstanceOf[ConsumedRecordData]
      val incomingContext = consumerRecordData.incomingContext()
      val incomingSpan = incomingContext.get(Span.Key)

      consumerSpan
        .tag("kafka.groupId", consumerRecordData.consumerInfo().groupId)
        .tag("kafka.clientId", consumerRecordData.consumerInfo().clientId)
        .tag("kafka.pollTime", consumerRecordData.nanosSincePollStart())

      if(!incomingSpan.isEmpty) {
        if (settings.continueTraceOnConsumer)
          consumerSpan.asChildOf(incomingSpan)
        else
          consumerSpan.link(incomingSpan, Span.Link.Kind.FollowsFrom)
      }
    }

    if (settings.useDelayedSpans)
      consumerSpan
        .delay(Kamon.clock().toInstant(record.timestamp() * 1000000))
        .start()
    else
      consumerSpan.start()
  }

  object Keys {
    val Null = "NULL"
    val ContextHeader = "kamon-context"
  }

  case class Settings (
    continueTraceOnConsumer: Boolean,
    useDelayedSpans: Boolean
  )
}
