package com.purbon.kafka.topology.topic;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.topic.ReplicationFactorValidation;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ReplicationFactorValidationTest {

  @Test(expected = ValidationException.class)
  public void shouldVerifyDifferentValuesWhenUsingEq()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 35, "eq");
    validation.valid(topic);
  }

  @Test(expected = ValidationException.class)
  public void shouldVerifyDifferentValuesWhenUsingGte()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 35, "gte");
    validation.valid(topic);
  }

  @Test(expected = ValidationException.class)
  public void shouldVerifyDifferentValuesWhenUsingLte()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 15, "lte");
    validation.valid(topic);
  }

  @Test(expected = ValidationException.class)
  public void shouldVerifyDifferentValuesWhenUsingGt()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 35, "gt");
    validation.valid(topic);
  }

  @Test(expected = ValidationException.class)
  public void shouldVerifyDifferentValuesWhenUsingLt()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 33, "lt");
    validation.valid(topic);
  }

  @Test
  public void shouldVerifyDifferentValuesWhenUsingEqSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 34, "eq");
    validation.valid(topic);
  }

  @Test
  public void shouldVerifyDifferentValuesWhenUsingGteSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 34, "gte");
    validation.valid(topic);
  }

  @Test
  public void shouldVerifyDifferentValuesWhenUsingLteSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 34, "lte");
    validation.valid(topic);
  }

  @Test
  public void shouldVerifyDifferentValuesWhenUsingGtSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 33, "gt");
    validation.valid(topic);
  }

  @Test
  public void shouldVerifyDifferentValuesWhenUsingLtSuccessfully()
      throws ValidationException, ConfigurationException {
    Map<String, String> config = new HashMap<>();
    config.put("replication.factor", "34");
    config.put("num.partitions", "123");

    Topic topic = new Topic("topic", config);
    ReplicationFactorValidation validation = new ReplicationFactorValidation((short) 35, "lt");
    validation.valid(topic);
  }
}
