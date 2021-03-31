package com.purbon.kafka.topology.validation.topology;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.validation.TopologyValidation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicNameRegexValidation implements TopologyValidation {

  private static final Logger LOGGER = LogManager.getLogger(TopicNameRegexValidation.class);

  private String topicNamePattern;

  public TopicNameRegexValidation() throws ConfigurationException {
    throw new ConfigurationException(
        "TopicNameRegexValidation is configured without specifying a topic name pattern");
  }

  public TopicNameRegexValidation(String pattern) {
    this.topicNamePattern = pattern;
    LOGGER.info(String.format("Applying Topic Name Regex Validation [%s]", pattern));
  }

  @Override
  public void valid(Topology topology) throws ValidationException {

    for (Project project : topology.getProjects()) {
      for (Topic topic : project.getTopics()) {
        matches(topic.toString());
      }
    }
  }

  public void matches(String topicName) throws ValidationException {
    if (!topicName.matches(topicNamePattern)) {
      String msg =
          String.format(
              "topic name '%s' does not follow the regex: %s", topicName, topicNamePattern);
      throw new ValidationException(msg);
    }
  }
}
