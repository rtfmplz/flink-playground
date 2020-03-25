package org.apache.flink.playgrounds.ops.clickcount;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.playgrounds.ops.clickcount.functions.BackpressureMap;
import org.apache.flink.playgrounds.ops.clickcount.functions.ClickEventStatisticsCollector;
import org.apache.flink.playgrounds.ops.clickcount.functions.CountingAggregator;
import org.apache.flink.playgrounds.ops.clickcount.functions.MyMetricCounter;
import org.apache.flink.playgrounds.ops.clickcount.functions.MyMetricHistogram;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventDeserializationSchema;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatistics;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatisticsSerializationSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
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
  private String outputTopic;


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
    outputTopic = params.get("output-topic", "output");
    final String brokers = params.get("bootstrap.servers", "localhost:9092");
    kafkaProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
    kafkaProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "click-event-count-" + System.currentTimeMillis());
  }

  private void process() throws Exception {

    DataStream<ClickEvent> clicks = env.addSource(
        new FlinkKafkaConsumer<>(inputTopic, new ClickEventDeserializationSchema(), kafkaProps))
        .name("ClickEvent Source")
        .assignTimestampsAndWatermarks(
            new BoundedOutOfOrdernessTimestampExtractor<ClickEvent>(
                Time.of(200, TimeUnit.MILLISECONDS)) {
              @Override
              public long extractTimestamp(final ClickEvent element) {
                return element.getTimestamp().getTime();
              }
            })
        .map(new MyMetricCounter())
        .map(new MyMetricHistogram());

    if (this.inflictBackpressure) {
      // Force a network shuffle so that the backpressure will affect the buffer pools
      clicks.keyBy(ClickEvent::getPage)
          .map(new BackpressureMap())
          .name("Backpressure");
    }

    DataStream<ClickEventStatistics> statistics = clicks
        .keyBy(ClickEvent::getPage)
        .timeWindow(WINDOW_SIZE)
        .aggregate(new CountingAggregator(),
            new ClickEventStatisticsCollector())
        .name("ClickEvent Counter");

    statistics
        .addSink(new FlinkKafkaProducer<>(
            outputTopic,
            new ClickEventStatisticsSerializationSchema(outputTopic),
            kafkaProps,
            FlinkKafkaProducer.Semantic.AT_LEAST_ONCE))
        .name("ClickEventStatistics Sink");

    env.execute("Click Event Count");
  }
}
