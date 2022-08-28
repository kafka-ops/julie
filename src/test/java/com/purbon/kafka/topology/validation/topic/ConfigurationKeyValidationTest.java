package com.purbon.kafka.topology.validation.topic;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import java.util.HashMap;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;

public class ConfigurationKeyValidationTest {

  @Test
  void testKoConfigValues() throws ValidationException {
    assertThrows(ValidationException.class, () -> {
      var config = new HashMap<String, String>();
      config.put("foo", "2");
      config.put(TopicConfig.COMPRESSION_TYPE_CONFIG, "gzip");
      Topic topic = new Topic("topic", config);

      ConfigurationKeyValidation validation = new ConfigurationKeyValidation();
      validation.valid(topic);
    });
  }

  @Test
  void testOkConfigValues() throws ValidationException {
    var config = new HashMap<String, String>();
    config.put(TopicConfig.COMPRESSION_TYPE_CONFIG, "gzip");
    Topic topic = new Topic("topic", config);

    ConfigurationKeyValidation validation = new ConfigurationKeyValidation();
    validation.valid(topic);
  }

  @Test
  void testPartitionsAndReplicationConfigValues() throws ValidationException {
    var config = new HashMap<String, String>();
    config.put("replication.factor", "3");
    Topic topic = new Topic("topic", config);

    ConfigurationKeyValidation validation = new ConfigurationKeyValidation();
    validation.valid(topic);
  }
}
