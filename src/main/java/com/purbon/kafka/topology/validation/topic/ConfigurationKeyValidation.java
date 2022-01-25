package com.purbon.kafka.topology.validation.topic;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.TopicValidation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This validation checks that all topic configs are valid ones according to the TopicConfig class.
 */
@NoArgsConstructor
@RequiredArgsConstructor
public class ConfigurationKeyValidation implements TopicValidation {

  private static final Logger LOGGER = LogManager.getLogger(ConfigurationKeyValidation.class);

  @NonNull private Configuration config;

  @Override
  public void valid(Topic topic) throws ValidationException {
    Field[] fields = TopicConfig.class.getDeclaredFields();
    TopicConfig topicConfig = new TopicConfig();
    Map<String, String> topicConfigMap = getTopicConfig(topic);
    for (Map.Entry<String, String> entry : topicConfigMap.entrySet()) {
      boolean match =
          Arrays.stream(fields)
              .anyMatch(
                  field -> {
                    try {
                      return ((String) field.get(topicConfig)).contains(entry.getKey());
                    } catch (IllegalAccessException e) {
                      LOGGER.error(e);
                      return false;
                    }
                  });
      if (!match) {
        String msg =
            String.format("Topic %s has an invalid configuration value: %s", topic, entry.getKey());
        throw new ValidationException(msg);
      }
    }
  }

  private Map<String, String> getTopicConfig(Topic topic) {
    Topic clonedTopic = ((Topic) topic).clone();
    return clonedTopic.getRawConfig();
  }
}
