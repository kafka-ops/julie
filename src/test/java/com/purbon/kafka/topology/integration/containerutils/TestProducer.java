package com.purbon.kafka.topology.integration.containerutils;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public final class TestProducer implements Closeable {

  private static final int NUM_VALUES_TO_PRODUCE = 3;
  private final KafkaProducer<String, String> producer;

  private TestProducer(final KafkaProducer<String, String> producer) {
    this.producer = producer;
  }

  public static TestProducer create(
      final AlternativeKafkaContainer container, final String usernameAndPassword) {
    return create(container.getBootstrapServers(), usernameAndPassword);
  }

  private static TestProducer create(
      final String bootstrapServers, final String usernameAndPassword) {
    final Map<String, Object> config =
        ContainerTestUtils.getSaslConfig(
            bootstrapServers, usernameAndPassword, usernameAndPassword);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.ACKS_CONFIG, "all");
    config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "3000");
    config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
    config.put(ProducerConfig.LINGER_MS_CONFIG, "0");
    config.put(ProducerConfig.RETRIES_CONFIG, "0");
    return new TestProducer(new KafkaProducer<>(config));
  }

  public void produce(final String topicName, final String recordValue) {
    produce(topicName, null, recordValue);
  }

  public void produce(final String topicName, final String key, final String recordValue) {
    final ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, recordValue);
    try {
      producer
          .send(
              record,
              (metadata, exception) -> {
                if (exception != null) {
                  throw (exception instanceof RuntimeException)
                      ? (RuntimeException) exception
                      : new RuntimeException(exception);
                }
              })
          .get(); // Make call synchronous, to be able to get exceptions in time.
    } catch (final InterruptedException | ExecutionException e) {
      final Throwable cause = e.getCause();
      throw (cause instanceof RuntimeException)
          ? (RuntimeException) cause
          : new RuntimeException(e);
    }
    producer.flush();
  }

  public Set<String> produceSomeStrings(final String topicName) {
    final Set<String> values = new HashSet<>();
    for (int q = 0; q < NUM_VALUES_TO_PRODUCE; q++) {
      final String value = (q + 1) + "-" + System.currentTimeMillis();
      values.add(value);
      produce(topicName, value);
    }
    return values;
  }

  @Override
  public void close() {
    producer.close();
  }
}
