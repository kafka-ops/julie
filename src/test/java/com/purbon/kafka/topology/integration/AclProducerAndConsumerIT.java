package com.purbon.kafka.topology.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.ContainerTestUtils;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.integration.containerutils.TestConsumer;
import com.purbon.kafka.topology.integration.containerutils.TestProducer;
import java.util.Set;
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public final class AclProducerAndConsumerIT {

  private static final String TOPIC = "producer-and-consumer-test-topic";
  private static final String OTHER_TOPIC = "other-" + TOPIC;
  private static final String CONSUMER_GROUP = "producer-and-consumer-test-consumer-group";
  private static final String UNKNOWN_USERNAME = "unknown-user";
  private static final String NO_ACCESS_USERNAME = "no-access-user";
  private static final String PRODUCER_USERNAME = "producer";
  private static final String CONSUMER_USERNAME = "consumer";
  private static final String OTHER_PRODUCER_USERNAME = "other-producer";
  private static final String OTHER_CONSUMER_USERNAME = "other-consumer";
  private static SaslPlaintextKafkaContainer container;

  @BeforeAll
  public static void beforeClass() {
    container =
        ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"))
            .withUser(NO_ACCESS_USERNAME)
            .withUser(PRODUCER_USERNAME)
            .withUser(CONSUMER_USERNAME)
            .withUser(OTHER_PRODUCER_USERNAME)
            .withUser(OTHER_CONSUMER_USERNAME);
    container.start();
    ContainerTestUtils.populateAcls(
        container, "/acl-producer-and-consumer-it.yaml", "/integration-tests.properties");
  }

  @AfterAll
  public static void afterClass() {
    container.stop();
  }

  @Test
  void shouldNotProduceWhenUnknownUser() {
    assertThrows(SaslAuthenticationException.class, () -> {
      try (final TestProducer producer = TestProducer.create(container, UNKNOWN_USERNAME)) {
        producer.produce(TOPIC, "foo");
      }
    });
  }

  @Test
  void shouldNotConsumeWhenUnknownUser() {
    assertThrows(SaslAuthenticationException.class, () -> {
      try (final TestConsumer consumer =
          TestConsumer.create(container, UNKNOWN_USERNAME, CONSUMER_GROUP)) {
        consumer.consumeForAWhile(TOPIC, null);
      }
    });
  }

  @Test
  void shouldNotProduceWithoutPermission() {
    assertThrows(TopicAuthorizationException.class, () -> {
      try (final TestProducer producer = TestProducer.create(container, NO_ACCESS_USERNAME)) {
        producer.produce(TOPIC, "foo");
      }
    });
  }

  @Test
  void shouldNotConsumeWithoutPermission() {
    assertThrows(TopicAuthorizationException.class, () -> {
      try (final TestConsumer consumer =
          TestConsumer.create(container, NO_ACCESS_USERNAME, CONSUMER_GROUP)) {
        consumer.consumeForAWhile(TOPIC, null);
      }
    });
  }

  @Test
  void shouldNotProduceWithoutPermissionEvenIfPermittedElsewhere() {
    assertThrows(TopicAuthorizationException.class, () -> {
      try (final TestProducer producer = TestProducer.create(container, OTHER_PRODUCER_USERNAME)) {
        producer.produce(TOPIC, "foo");
      }
    });
  }

  @Test
  void shouldNotConsumeWithoutPermissionEvenIfPermittedElsewhere() {
    assertThrows(TopicAuthorizationException.class, () -> {
      try (final TestConsumer consumer =
          TestConsumer.create(container, OTHER_CONSUMER_USERNAME, CONSUMER_GROUP)) {
        consumer.consumeForAWhile(TOPIC, null);
      }
    });
  }

  @Test
  void shouldNotProduceWhenConsumer() {
    assertThrows(TopicAuthorizationException.class, () -> {
      try (final TestProducer producer = TestProducer.create(container, CONSUMER_USERNAME)) {
        producer.produce(TOPIC, "foo");
      }
    });
  }

  @Test
  void shouldNotConsumeWhenProducer() {
    assertThrows(GroupAuthorizationException.class, () -> {
      try (final TestConsumer consumer =
          TestConsumer.create(container, PRODUCER_USERNAME, CONSUMER_GROUP)) {
        consumer.consumeForAWhile(TOPIC, null);
      }
    });
  }

  @Test
  void shouldProduceAndConsume() {
    produceAndConsume(TOPIC, PRODUCER_USERNAME, CONSUMER_USERNAME);
  }

  @Test
  void shouldProduceAndConsumeElsewhere() {
    /* Just to test that everything was spelled correctly in source and config for the above tests. */
    produceAndConsume(OTHER_TOPIC, OTHER_PRODUCER_USERNAME, OTHER_CONSUMER_USERNAME);
  }

  private void produceAndConsume(
      final String topicName, final String producerUsername, final String consumerUsername) {
    try (final TestProducer producer = TestProducer.create(container, producerUsername);
        final TestConsumer consumer =
            TestConsumer.create(container, consumerUsername, CONSUMER_GROUP)) {
      final Set<String> values = producer.produceSomeStrings(topicName);
      consumer.consumeForAWhile(
          topicName,
          (key, value) -> {
            values.remove(value);
            return !values.isEmpty();
          });
      if (!values.isEmpty()) {
        fail("Unable to consume all messages.");
      }
    }
  }
}
