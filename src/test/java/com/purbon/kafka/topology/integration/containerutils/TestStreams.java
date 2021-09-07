package com.purbon.kafka.topology.integration.containerutils;

import java.io.Closeable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

public final class TestStreams implements Closeable {

  private final KafkaStreams streams;
  private boolean topicAuthorizationExceptionThrown;

  private TestStreams(final KafkaStreams streams) {
    this.streams = streams;
  }

  public static TestStreams create(
      final AlternativeKafkaContainer container,
      final String usernameAndPassword,
      final String applicationId,
      Topology topology) {
    return create(container.getBootstrapServers(), usernameAndPassword, applicationId, topology);
  }

  private static TestStreams create(
      final String bootstrapServers,
      final String usernameAndPassword,
      final String applicationId,
      Topology topology) {
    final Map<String, Object> config =
        ContainerTestUtils.getSaslConfig(
            bootstrapServers, usernameAndPassword, usernameAndPassword);
    config.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
    config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    config.put(
        StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    config.put(StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");

    Properties properties = new Properties();
    properties.putAll(config);
    return new TestStreams(new KafkaStreams(topology, properties));
  }

  public void start() {
    setUncaughtExceptionHandler(
        (t, e) ->
            topicAuthorizationExceptionThrown =
                ExceptionUtils.indexOfType(e, TopicAuthorizationException.class) > 0);
    streams.start();
  }

  @Override
  public void close() {
    streams.close();
  }

  public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
    streams.setUncaughtExceptionHandler(eh);
  }

  public boolean isTopicAuthorizationExceptionThrown() {
    return topicAuthorizationExceptionThrown;
  }
}
