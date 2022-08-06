package com.purbon.kafka.topology.topic;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.topic.TopicNameRegexValidation;
import org.junit.Test;

public class TopicNameRegexValidationTest {

  @Test(expected = ValidationException.class)
  public void testKoConfigValues() throws ValidationException, ConfigurationException {
    Topic topic = new Topic("topic");
    TopicNameRegexValidation validation = new TopicNameRegexValidation("[1-9]");
    validation.valid(topic);
  }

  @Test
  public void testOkConfigValues() throws ValidationException, ConfigurationException {
    Topic topic = new Topic("topic");
    TopicNameRegexValidation validation = new TopicNameRegexValidation("[a-z]*");
    validation.valid(topic);
  }

  @Test(expected = ConfigurationException.class)
  public void testEmptyParam() throws ValidationException, ConfigurationException {
    Topic topic = new Topic("topic");
    TopicNameRegexValidation validation = new TopicNameRegexValidation("");
    validation.valid(topic);
  }
}
