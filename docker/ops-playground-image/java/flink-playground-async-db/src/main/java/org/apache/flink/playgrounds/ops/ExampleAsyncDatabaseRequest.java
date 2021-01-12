package org.apache.flink.playgrounds.ops;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.playgrounds.ops.function.AsyncDatabaseRequest;
import org.apache.flink.playgrounds.ops.records.ClickEvent;
import org.apache.flink.playgrounds.ops.records.ClickEventDeserializationSchema;
import org.apache.flink.playgrounds.ops.records.Database;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

public class ExampleAsyncDatabaseRequest {

  private static final String CHECKPOINTING_OPTION = "checkpointing";
  private static final String EVENT_TIME_OPTION = "event-time";
  private Properties kafkaProps = new Properties();
  private StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

  private String inputTopic;

  private ExampleAsyncDatabaseRequest(String... args) {
    initialize(args);
  }

  public static void main(String[] args) throws Exception {
    new ExampleAsyncDatabaseRequest(args).process();
  }

  private void initialize(String... args) {
    final ParameterTool params = ParameterTool.fromArgs(args);

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
    final String brokers = params.get("bootstrap.servers", "localhost:9092");
    final String consumer = params.get("kafka.consumer", "gary");

    kafkaProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, consumer);
    kafkaProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);

    // EXACTLY_ONCE
    kafkaProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    kafkaProps.setProperty(ProducerConfig.ACKS_CONFIG, "all");
    kafkaProps.setProperty(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, "300");
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
        .name("ClickEvent Source");

    DataStream<String> mysql = AsyncDataStream
        .unorderedWait(clicks.map(ClickEvent::getPage), new AsyncDatabaseRequest(Database.MYSQL),
            1000, TimeUnit.MILLISECONDS, 100)
        .name("MYSQL");

    DataStream<String> clicks2 = AsyncDataStream
        .unorderedWait(mysql, new AsyncDatabaseRequest(Database.POSTGRES), 1000,
            TimeUnit.MILLISECONDS, 100)
        .name("POSTGRES");

    clicks2.print();

    env.execute("async-db");
  }
}
