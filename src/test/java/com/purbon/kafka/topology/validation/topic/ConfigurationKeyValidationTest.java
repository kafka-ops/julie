package com.purbon.kafka.topology.validation.topic;

import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Topic;
import java.util.HashMap;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.Test;

public class ConfigurationKeyValidationTest {

  @Test(expected = ValidationException.class)
  public void testKoConfigValues() throws ValidationException {
    HashMap<String, String> config = new HashMap<String, String>();
    config.put("foo", "2");
    config.put(TopicConfig.COMPRESSION_TYPE_CONFIG, "gzip");
    Topic topic = new TopicImpl("topic", config);

    ConfigurationKeyValidation validation = new ConfigurationKeyValidation();
    validation.valid(topic);
  }

  @Test
  public void testOkConfigValues() throws ValidationException {
    HashMap<String, String> config = new HashMap<String, String>();
    config.put(TopicConfig.COMPRESSION_TYPE_CONFIG, "gzip");
    Topic topic = new TopicImpl("topic", config);

    ConfigurationKeyValidation validation = new ConfigurationKeyValidation();
    validation.valid(topic);
  }

  @Test
  public void testPartitionsAndReplicationConfigValues() throws ValidationException {
    HashMap<String, String> config = new HashMap<String, String>();
    config.put("replication.factor", "3");
    Topic topic = new TopicImpl("topic", config);

    ConfigurationKeyValidation validation = new ConfigurationKeyValidation();
    validation.valid(topic);
  }
}
