package com.purbon.kafka.topology.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.ContainerTestUtils;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.integration.containerutils.TestConsumer;
import com.purbon.kafka.topology.integration.containerutils.TestProducer;
import com.purbon.kafka.topology.integration.containerutils.TestStreams;
import java.util.Set;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public final class StreamsAclIT {

  public static final String TOPIC_A = "topic-A";
  public static final String TOPIC_B = "topic-B";
  public static final String TOPIC_C = "topic-C";
  public static final long MAX_TEST_SEC_BEFORE_GIVING_UP = 60;
  public static final String STREAMS_APP_ID = "streams-appid";
  private static final String PRODUCER_USERNAME = "producer";
  private static final String CONSUMER_USERNAME = "consumer";
  private static final String STREAMS_USERNAME = "streamsapp";
  private static final String CONSUMER_GROUP = "streams-consumer-test-consumer-group";

  private static SaslPlaintextKafkaContainer container;

  @BeforeClass
  public static void beforeClass() {
    container =
        ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"))
            .withUser(PRODUCER_USERNAME)
            .withUser(CONSUMER_USERNAME)
            .withUser(STREAMS_USERNAME);
    container.start();
    ContainerTestUtils.populateAcls(
        container, "/streams-acl-it.yaml", "/integration-tests.properties");
  }

  @AfterClass
  public static void afterClass() {
    container.stop();
  }

  @Test
  public void shouldNotProduceWithoutPermission() {
    try (final TestProducer producer = TestProducer.create(container, PRODUCER_USERNAME)) {
      producer.produceSomeStrings(TOPIC_A);
    }
    final StreamsBuilder builder = new StreamsBuilder();
    KStream<Object, Object> source = builder.stream(TOPIC_A);
    source.filter((key, val) -> true).to(TOPIC_C);

    final TestStreams streams =
        TestStreams.create(container, STREAMS_USERNAME, STREAMS_APP_ID, builder.build());
    streams.start();

    await()
        .atMost(MAX_TEST_SEC_BEFORE_GIVING_UP, SECONDS)
        .until(streams::isTopicAuthorizationExceptionThrown);

    streams.close();
  }

  @Test
  public void testSimpleStream() {
    Set<String> values;
    try (final TestProducer producer = TestProducer.create(container, PRODUCER_USERNAME)) {
      values = producer.produceSomeStrings(TOPIC_A);
    }

    final StreamsBuilder builder = new StreamsBuilder();
    KStream<Object, Object> source = builder.stream(TOPIC_A);
    source.filter((key, val) -> values.stream().anyMatch(v -> v.equals(val))).to(TOPIC_B);

    final TestStreams streams =
        TestStreams.create(container, STREAMS_USERNAME, STREAMS_APP_ID, builder.build());
    streams.start();

    try (final TestConsumer consumer =
        TestConsumer.create(container, CONSUMER_USERNAME, CONSUMER_GROUP)) {
      consumer.consumeForAWhile(
          TOPIC_B,
          (key, value) -> {
            values.remove(value);
            return !values.isEmpty();
          });
    }
    streams.close();

    assertThat(values).isEmpty();
  }

  @Test
  public void testStreamWithInternalTopics() {
    final Set<String> values;
    try (final TestProducer producer = TestProducer.create(container, PRODUCER_USERNAME)) {
      values = producer.produceSomeStrings(TOPIC_A);
    }

    final StreamsBuilder builder = new StreamsBuilder();
    KStream<String, String> source = builder.stream(TOPIC_A);

    // Just re-group by using value as key, groupBy requires new internal topics
    source
        .groupBy((key, value) -> value)
        .aggregate(() -> "", (aggKey, newValue, aggValue) -> newValue)
        .filter((key, val) -> values.stream().anyMatch(v -> v.equals(key)))
        .toStream()
        .to(TOPIC_B);

    final TestStreams streams =
        TestStreams.create(container, STREAMS_USERNAME, STREAMS_APP_ID, builder.build());
    streams.start();

    try (final TestConsumer consumer =
        TestConsumer.create(container, CONSUMER_USERNAME, CONSUMER_GROUP)) {
      consumer.consumeForAWhile(
          TOPIC_B,
          (key, value) -> {
            values.remove(key);
            return !values.isEmpty();
          });
    }
    streams.close();

    assertThat(values).isEmpty();
  }
}
