package com.purbon.kafka.topology.topic;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.topic.MinInSyncReplicasValidation;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class MinInSyncReplicasValidationTest {

  @Test(expected = ValidationException.class)
  public void shouldCheckKoValuesSuccessfully() throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "3");
    config.put("min.insync.replicas", "3");

    Topic topic = new Topic("topic", config);
    MinInSyncReplicasValidation validation = new MinInSyncReplicasValidation();
    validation.valid(topic);
  }

  @Test
  public void shouldCheckMinimalValuesSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "3");
    config.put("min.insync.replicas", "1");

    Topic topic = new Topic("topic", config);
    MinInSyncReplicasValidation validation = new MinInSyncReplicasValidation();
    validation.valid(topic);
  }

  @Test
  public void shouldCheckOkValuesSuccessfully() throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "3");
    config.put("min.insync.replicas", "2");

    Topic topic = new Topic("topic", config);
    MinInSyncReplicasValidation validation = new MinInSyncReplicasValidation();
    validation.valid(topic);
  }

  @Test
  public void shouldCheckMissingMinInSyncValuesSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "3");

    Topic topic = new Topic("topic", config);
    MinInSyncReplicasValidation validation = new MinInSyncReplicasValidation();
    validation.valid(topic);
  }
}
