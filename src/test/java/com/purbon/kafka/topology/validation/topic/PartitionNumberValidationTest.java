package com.purbon.kafka.topology.validation.topic;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PartitionNumberValidationTest {

  @Test
  void shouldVerifyDifferentValuesWhenUsingEq() throws ValidationException, ConfigurationException {
    assertThrows(
        ValidationException.class,
        () -> {
          Map<String, String> config = new HashMap<>();
          config.put("replication.factor", "34");
          config.put("num.partitions", "123");

          Topic topic = new Topic("topic", config);
          PartitionNumberValidation validation = new PartitionNumberValidation((short) 35, "eq");
          validation.valid(topic);
        });
  }

  @Test
  void shouldVerifyDifferentValuesWhenUsingGte()
      throws ValidationException, ConfigurationException {
    assertThrows(
        ValidationException.class,
        () -> {
          Map<String, String> config = new HashMap<>();
          config.put("replication.factor", "34");
          config.put("num.partitions", "123");

          Topic topic = new Topic("topic", config);
          PartitionNumberValidation validation = new PartitionNumberValidation((short) 124, "gte");
          validation.valid(topic);
        });
  }

  @Test
  void shouldVerifyDifferentValuesWhenUsingLte()
      throws ValidationException, ConfigurationException {
    assertThrows(
        ValidationException.class,
        () -> {
          Map<String, String> config = new HashMap<>();
          config.put("replication.factor", "34");
          config.put("num.partitions", "123");

          Topic topic = new Topic("topic", config);
          PartitionNumberValidation validation = new PartitionNumberValidation((short) 15, "lte");
          validation.valid(topic);
        });
  }

  @Test
  void shouldVerifyDifferentValuesWhenUsingGt() throws ValidationException, ConfigurationException {
    assertThrows(
        ValidationException.class,
        () -> {
          Map<String, String> config = new HashMap<>();
          config.put("replication.factor", "34");
          config.put("num.partitions", "123");

          Topic topic = new Topic("topic", config);
          PartitionNumberValidation validation = new PartitionNumberValidation((short) 125, "gt");
          validation.valid(topic);
        });
  }

  @Test
  void shouldVerifyDifferentValuesWhenUsingLt() throws ValidationException, ConfigurationException {
    assertThrows(
        ValidationException.class,
        () -> {
          Map<String, String> config = new HashMap<>();
          config.put("replication.factor", "34");
          config.put("num.partitions", "123");

          Topic topic = new Topic("topic", config);
          PartitionNumberValidation validation = new PartitionNumberValidation((short) 33, "lt");
          validation.valid(topic);
        });
  }

  @Test
  void shouldVerifyDifferentValuesWhenUsingEqSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    PartitionNumberValidation validation = new PartitionNumberValidation((short) 123, "eq");
    validation.valid(topic);
  }

  @Test
  void shouldVerifyDifferentValuesWhenUsingGteSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    PartitionNumberValidation validation = new PartitionNumberValidation((short) 34, "gte");
    validation.valid(topic);
  }

  @Test
  void shouldVerifyDifferentValuesWhenUsingLteSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    PartitionNumberValidation validation = new PartitionNumberValidation((short) 123, "lte");
    validation.valid(topic);
  }

  @Test
  void shouldVerifyDifferentValuesWhenUsingGtSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    PartitionNumberValidation validation = new PartitionNumberValidation((short) 122, "gt");
    validation.valid(topic);
  }

  @Test
  void shouldVerifyDifferentValuesWhenUsingLtSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    PartitionNumberValidation validation = new PartitionNumberValidation((short) 124, "lt");
    validation.valid(topic);
  }
}
