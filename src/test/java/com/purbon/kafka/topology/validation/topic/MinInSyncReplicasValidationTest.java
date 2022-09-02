package com.purbon.kafka.topology.validation.topic;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MinInSyncReplicasValidationTest {

  @Test
  void shouldCheckKoValuesSuccessfully() throws ValidationException, ConfigurationException {
    assertThrows(
        ValidationException.class,
        () -> {
          Map<String, String> config = new HashMap<>();
          config.put("replication.factor", "3");
          config.put("min.insync.replicas", "3");

          Topic topic = new Topic("topic", config);
          MinInSyncReplicasValidation validation = new MinInSyncReplicasValidation();
          validation.valid(topic);
        });
  }

  @Test
  void shouldCheckMinimalValuesSuccessfully() throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "3");
    config.put("min.insync.replicas", "1");

    Topic topic = new Topic("topic", config);
    MinInSyncReplicasValidation validation = new MinInSyncReplicasValidation();
    validation.valid(topic);
  }

  @Test
  void shouldCheckOkValuesSuccessfully() throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "3");
    config.put("min.insync.replicas", "2");

    Topic topic = new Topic("topic", config);
    MinInSyncReplicasValidation validation = new MinInSyncReplicasValidation();
    validation.valid(topic);
  }

  @Test
  void shouldCheckMissingMinInSyncValuesSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "3");

    Topic topic = new Topic("topic", config);
    MinInSyncReplicasValidation validation = new MinInSyncReplicasValidation();
    validation.valid(topic);
  }
}
