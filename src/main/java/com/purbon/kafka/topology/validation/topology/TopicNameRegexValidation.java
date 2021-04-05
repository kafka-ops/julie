package com.purbon.kafka.topology.validation.topology;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.validation.TopologyValidation;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

public class TopicNameRegexValidation implements TopologyValidation {

  private static final Logger LOGGER = LogManager.getLogger(TopicNameRegexValidation.class);

  private String topicNamePattern;

  public TopicNameRegexValidation() throws ConfigurationException {
	  Config config = ConfigFactory.load();
	  try {
		  this.topicNamePattern = config.getString("topology.validations.regexp");
	  } catch (ConfigException e) {
		  throw new ConfigurationException(
			        "TopicNameRegexValidation requires you to define your regex in config 'topology.validations.regexp'");
	  }

	  if (StringUtils.isBlank(topicNamePattern)) {
		  throw new ConfigurationException(
	        "TopicNameRegexValidation is configured without specifying a topic name pattern. Use config 'topology.validations.regexp'");
	  }
	  
	  try {
          Pattern.compile(topicNamePattern);
      } catch (PatternSyntaxException exception) {
		  throw new ConfigurationException(
			        String.format("TopicNameRegexValidation configured with unvalid regex '%s'", topicNamePattern));
      }
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
              "Topic name '%s' does not follow regex: %s", topicName, topicNamePattern);
      throw new ValidationException(msg);
    }
  }
}
