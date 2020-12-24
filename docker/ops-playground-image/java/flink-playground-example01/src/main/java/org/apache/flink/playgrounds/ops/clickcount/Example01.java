package org.apache.flink.playgrounds.ops.clickcount;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

@SuppressWarnings("checkstyle:LineLength")
public class Example01 {

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


  private Example01(String... args) {
    initialize(args);
  }

  public static void main(String[] args) throws Exception {
    new Example01(args).process();
  }

  private void initialize(String... args) {

  }

  private void process() throws Exception {

    env.fromElements("hello world", "hello world", "hello world")
        .flatMap(new Tokenizer())
        .keyBy(0)
        .sum(1)
        .print();

    env.execute("example01");
  }

  public static final class Tokenizer implements FlatMapFunction<String, Tuple2<String, Integer>> {

    @Override
    public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
      // normalize and split the line
      String[] tokens = value.toLowerCase().split("\\W+");

      // emit the pairs
      for (String token : tokens) {
        if (token.length() > 0) {
          out.collect(new Tuple2<>(token, 1));
        }
      }
    }
  }
}
