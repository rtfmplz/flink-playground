package org.apache.flink.playgrounds.ops.clickcount;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.playgrounds.ops.clickcount.functions.BackpressureMap;
import org.apache.flink.playgrounds.ops.clickcount.functions.ClickEventStatisticsCollector;
import org.apache.flink.playgrounds.ops.clickcount.functions.CountingAggregator;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventDeserializationSchema;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventSerializationSchema;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatistics;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatisticsSerializationSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

/**
 * A simple streaming job reading {@link ClickEvent}s from Kafka, counting events per 15 seconds and
 * writing the resulting {@link ClickEventStatistics} back to Kafka.
 *
 * <p> It can be run with or without checkpointing and with event time or processing time
 * semantics.
 * </p>
 *
 * <p>The Job can be configured via the command line:</p>
 * * "--checkpointing": enables checkpointing * "--event-time": set the StreamTimeCharacteristic to
 * EventTime * "--backpressure": insert an operator that causes periodic backpressure *
 * "--input-topic": the name of the Kafka Topic to consume {@link ClickEvent}s from *
 * "--output-topic": the name of the Kafka Topic to produce {@link ClickEventStatistics} to *
 * "--bootstrap.servers": comma-separated list of Kafka brokers
 */
@SuppressWarnings("checkstyle:LineLength")
public class ClickEventCount {

  public static final Time WINDOW_SIZE = Time.of(15, TimeUnit.SECONDS);
  private static final String CHECKPOINTING_OPTION = "checkpointing";
  private static final String EVENT_TIME_OPTION = "event-time";
  private static final String BACKPRESSURE_OPTION = "backpressure";
  private Properties kafkaProps = new Properties();
  private StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

  private boolean inflictBackpressure;
  private String inputTopic;
  private String statisticsTopic;
  private String clickEventTopic;


  private ClickEventCount(String... args) {
    initialize(args);
  }

  public static void main(String[] args) throws Exception {
    new ClickEventCount(args).process();
  }

  private void initialize(String... args) {
    final ParameterTool params = ParameterTool.fromArgs(args);

    inflictBackpressure = params.has(BACKPRESSURE_OPTION);
    boolean checkpointingEnabled = params.has(CHECKPOINTING_OPTION);
    boolean eventTimeSemantics = params.has(EVENT_TIME_OPTION);

    if (checkpointingEnabled) {
      env.enableCheckpointing(1000);
    }

    if (eventTimeSemantics) {
      env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
    }

    //disabling Operator chaining to make it easier to follow the Job in the WebUI
    env.disableOperatorChaining();

    inputTopic = params.get("input-topic", "input");
    statisticsTopic = params.get("output-topic", "output");
    final String brokers = params.get("bootstrap.servers", "localhost:9092");
    final String consumer = params.get("kafka.consumer", "gary");

    kafkaProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, consumer);
    kafkaProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);

    // EXACTLY_ONCE
    kafkaProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    kafkaProps.setProperty(ProducerConfig.ACKS_CONFIG, "all");
    kafkaProps.setProperty(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, "300");

    clickEventTopic = consumer;
  }

  private void process() throws Exception {

    DataStream<ClickEvent> clicks = env.addSource(
        new FlinkKafkaConsumer<>(inputTopic, new ClickEventDeserializationSchema(), kafkaProps))
        .assignTimestampsAndWatermarks(
            new BoundedOutOfOrdernessTimestampExtractor<ClickEvent>(
                Time.of(200, TimeUnit.MILLISECONDS)) {
              @Override
              public long extractTimestamp(final ClickEvent element) {
                return element.getTimestamp().getTime();
              }
            })
//        .map(new MyMetricCounter())
//        .map(new MyMetricHistogram())
        .name("ClickEvent Source");

    if (this.inflictBackpressure) {
      // Force a network shuffle so that the backpressure will affect the buffer pools
      clicks.keyBy(ClickEvent::getPage)
          .map(new BackpressureMap())
          .name("Backpressure");
    }

    clicks.addSink(new FlinkKafkaProducer<ClickEvent>(
        clickEventTopic,
        new ClickEventSerializationSchema(clickEventTopic),
        kafkaProps,
        Semantic.EXACTLY_ONCE))
        .name("ClickEvent Sink");

    DataStream<ClickEventStatistics> statistics = clicks
        .keyBy(ClickEvent::getPage)
        .timeWindow(WINDOW_SIZE)
        .aggregate(new CountingAggregator(), new ClickEventStatisticsCollector())
        .name("ClickEvent Counter");

    statistics.addSink(new FlinkKafkaProducer<>(
        statisticsTopic,
        new ClickEventStatisticsSerializationSchema(statisticsTopic),
        kafkaProps,
        FlinkKafkaProducer.Semantic.EXACTLY_ONCE))
        .name("ClickEventStatistics Sink");

    env.execute("Click Event Count");
  }
}
