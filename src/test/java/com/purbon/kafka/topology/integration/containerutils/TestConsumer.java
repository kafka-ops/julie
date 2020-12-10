package com.purbon.kafka.topology.integration.containerutils;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public final class TestConsumer implements Closeable {

  /* This timeout includes the time spent fetching meta data, which may actually be several hundred ms.
   * If the timeout is too short, the poll function will return after fetching meta data, but before
   * actually trying to access the topic, thus not throwing the exception we are looking for. */
  private static final long MAX_MS_TO_CONSUME = 60 * 1000L;
  private final KafkaConsumer<String, String> consumer;

  public interface RecordHandler {

    /**
     * @return <code>true</code> to continue processing, <code>false</code> if no more records are
     *     requested.
     */
    boolean handle(String key, String value);
  }

  private TestConsumer(final KafkaConsumer<String, String> consumer) {
    this.consumer = consumer;
  }

  public static TestConsumer create(
      final AlternativeKafkaContainer container,
      final String usernameAndPassword,
      final String consumerGroup) {
    return create(container.getBootstrapServers(), usernameAndPassword, consumerGroup);
  }

  private static TestConsumer create(
      final String bootstrapServers, final String usernameAndPassword, final String consumerGroup) {
    final Map<String, Object> config =
        ContainerTestUtils.getSaslConfig(
            bootstrapServers, usernameAndPassword, usernameAndPassword);
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    return new TestConsumer(new KafkaConsumer<>(config));
  }

  public void consumeForAWhile(final String topicName, final RecordHandler handler) {
    consumer.subscribe(Collections.singleton(topicName));
    final long endTime = System.currentTimeMillis() + MAX_MS_TO_CONSUME;
    boolean continueConsuming = true;
    while (continueConsuming && System.currentTimeMillis() < endTime) {
      final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
      for (final ConsumerRecord<String, String> record : records) {
        if (handler != null) {
          continueConsuming = handler.handle(record.key(), record.value());
        }
        consumer.commitAsync();
      }
    }
  }

  @Override
  public void close() {
    consumer.close();
  }
}
