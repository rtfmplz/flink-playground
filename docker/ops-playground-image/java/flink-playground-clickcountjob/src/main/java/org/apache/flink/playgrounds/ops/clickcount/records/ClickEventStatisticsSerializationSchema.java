package org.apache.flink.playgrounds.ops.clickcount.records;


import javax.annotation.Nullable;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * A Kafka {@link KafkaSerializationSchema} to serialize {@link ClickEventStatistics}s as JSON.
 */
public class ClickEventStatisticsSerializationSchema implements
    KafkaSerializationSchema<ClickEventStatistics> {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private String topic;

  public ClickEventStatisticsSerializationSchema() {
  }

  public ClickEventStatisticsSerializationSchema(String topic) {
    this.topic = topic;
  }

  @Override
  public ProducerRecord<byte[], byte[]> serialize(
      final ClickEventStatistics message, @Nullable final Long timestamp) {
    try {
      //if topic is null, default topic will be used
      return new ProducerRecord<>(topic, objectMapper.writeValueAsBytes(message));
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Could not serialize record: " + message, e);
    }
  }
}
