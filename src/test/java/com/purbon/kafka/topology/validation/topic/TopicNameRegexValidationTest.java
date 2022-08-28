package com.purbon.kafka.topology.validation.topic;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import org.junit.jupiter.api.Test;

public class TopicNameRegexValidationTest {

  @Test
  void testKoConfigValues() throws ValidationException, ConfigurationException {
    assertThrows(ValidationException.class, () -> {
      Topic topic = new Topic("topic");
      TopicNameRegexValidation validation = new TopicNameRegexValidation("[1-9]");
      validation.valid(topic);
    });
  }

  @Test
  void testOkConfigValues() throws ValidationException, ConfigurationException {
    Topic topic = new Topic("topic");
    TopicNameRegexValidation validation = new TopicNameRegexValidation("[a-z]*");
    validation.valid(topic);
  }

  @Test
  void testEmptyParam() throws ValidationException, ConfigurationException {
    assertThrows(ConfigurationException.class, () -> {
      Topic topic = new Topic("topic");
      TopicNameRegexValidation validation = new TopicNameRegexValidation("");
      validation.valid(topic);
    });
  }
}
