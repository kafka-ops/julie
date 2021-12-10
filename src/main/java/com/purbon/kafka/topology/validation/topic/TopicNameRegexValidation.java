package com.purbon.kafka.topology.validation.topic;

import static com.purbon.kafka.topology.Constants.TOPOLOGY_VALIDATIONS_TOPIC_NAME_REGEXP;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.TopicValidation;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicNameRegexValidation implements TopicValidation {

  private static final Logger LOGGER = LogManager.getLogger(TopicNameRegexValidation.class);

  private String topicNamePattern;

  public TopicNameRegexValidation() throws ConfigurationException {
    this(getTopicNamePatternFromConfig());
  }

  public TopicNameRegexValidation(String pattern) throws ConfigurationException {
    validateRegexpPattern(pattern);

    this.topicNamePattern = pattern;
  }

  @Override
  public void valid(Topic topic) throws ValidationException {
    LOGGER.trace(String.format("Applying Topic Name Regex Validation [%s]", topicNamePattern));

    if (!topic.getName().matches(topicNamePattern)) {
      String msg =
          String.format("Topic name '%s' does not follow regex: %s", topic, topicNamePattern);
      throw new ValidationException(msg);
    }
  }

  private static String getTopicNamePatternFromConfig() throws ConfigurationException {
    Config config = ConfigFactory.load();
    try {
      return config.getString(TOPOLOGY_VALIDATIONS_TOPIC_NAME_REGEXP);
    } catch (ConfigException e) {
      String msg =
          String.format(
              "TopicNameRegexValidation requires you to define your regex in config '%s'",
              TOPOLOGY_VALIDATIONS_TOPIC_NAME_REGEXP);
      throw new ConfigurationException(msg);
    }
  }

  private void validateRegexpPattern(String pattern) throws ConfigurationException {
    if (StringUtils.isBlank(pattern)) {
      throw new ConfigurationException(
          "TopicNameRegexValidation is configured without specifying a topic name pattern. Use config 'topology.validations.regexp'");
    }

    try {
      Pattern.compile(pattern);
    } catch (PatternSyntaxException exception) {
      throw new ConfigurationException(
          String.format("TopicNameRegexValidation configured with invalid regex '%s'", pattern));
    }
  }
}
