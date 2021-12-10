package com.purbon.kafka.topology.integration.containerutils;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * A piece of Producer/Consumer for quota test.... Need to be a bit more long messages to send to
 * gain some bytes sizes to test quotas
 */
public final class TestProducerConsumerQuota implements Closeable {

  private static final long MAX_MS_TO_CONSUME = 60000 * 1000L;
  private static final long SIZE_BYTES_MESSAGE = 1024;
  private final KafkaConsumer<String, String> consumer;
  private final KafkaProducer<String, String> producer;

  public TestProducerConsumerQuota(
      final KafkaConsumer<String, String> consumer, final KafkaProducer<String, String> producer) {
    this.consumer = consumer;
    this.producer = producer;
  }

  private static TestProducerConsumerQuota create(
      final String bootstrapServers, final String usernameAndPassword, final String consumerGroup) {
    final Map<String, Object> config =
        ContainerTestUtils.getSaslConfig(
            bootstrapServers, usernameAndPassword, usernameAndPassword);
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    return new TestProducerConsumerQuota(new KafkaConsumer<>(config), null);
  }

  public void consumption(String topicName) {

    consumer.subscribe(Collections.singleton(topicName));

    final long endTime = System.currentTimeMillis() + MAX_MS_TO_CONSUME;
    boolean continueConsuming = true;
    while (continueConsuming && System.currentTimeMillis() < endTime) {
      final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100000));

      for (final ConsumerRecord<String, String> record : records) {
        consumer.commitAsync();
      }

      for (Map.Entry<MetricName, ? extends Metric> entry : consumer.metrics().entrySet()) {
        if ("fetch-throttle-time-avg".equals(entry.getKey().name())) {
          System.out.println(entry.getKey().name() + "->" + entry.getValue().metricValue());
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    consumer.close();
  }
}
