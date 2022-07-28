package com.purbon.kafka.topology.validation.topic;

import static com.purbon.kafka.topology.Constants.TOPOLOGY_VALIDATIONS_REPLICATION_FACTOR_OP;
import static com.purbon.kafka.topology.Constants.TOPOLOGY_VALIDATIONS_REPLICATION_FACTOR_VALUE;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.TopicValidation;
import com.typesafe.config.ConfigException;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class ReplicationFactorValidation implements TopicValidation {

  private final short replicationFactorValue;
  private final String replicationFactorOp;
  private Configuration config;

  public ReplicationFactorValidation(Configuration config) throws ConfigurationException {
    this(getReplicationFactor(config), getReplicationFactorOp(config));
    this.config = config;
  }

  public ReplicationFactorValidation(short replicationFactorValue, String replicationFactorOp) {
    this.replicationFactorValue = replicationFactorValue;
    this.replicationFactorOp = replicationFactorOp;
  }

  @Override
  public void valid(Topic topic) throws ValidationException {
    if (topic.replicationFactor().isPresent()
        && !validateReplicationFactor(topic.replicationFactor().get())) {
      String msg =
          String.format(
              "Topic %s has an unexpected replication factor: %s",
              topic, topic.replicationFactor().get());
      throw new ValidationException(msg);
    }
  }

  private boolean validateReplicationFactor(short topicValue) throws ValidationException {
    boolean result;
    switch (replicationFactorOp) {
      case "gt":
        result = topicValue > replicationFactorValue;
        break;
      case "lt":
        result = topicValue < replicationFactorValue;
        break;
      case "eq":
        result = topicValue == replicationFactorValue;
        break;
      case "gte":
        result = topicValue >= replicationFactorValue;
        break;
      case "lte":
        result = topicValue <= replicationFactorValue;
        break;
      case "ne":
        result = topicValue != replicationFactorValue;
        break;
      default:
        throw new ValidationException("Invalid Operation code in use " + replicationFactorOp);
    }
    return result;
  }

  private static String getReplicationFactorOp(Configuration config) throws ConfigurationException {
    List<String> validOpCodes = Arrays.asList("gt", "lt", "eq", "gte", "lte", "ne");
    try {
      String opCode =
          config.getProperty(TOPOLOGY_VALIDATIONS_REPLICATION_FACTOR_OP).toLowerCase().strip();
      if (!validOpCodes.contains(opCode)) {
        throw new ConfigException.BadValue(
            TOPOLOGY_VALIDATIONS_REPLICATION_FACTOR_OP, "Not supported operation value");
      }
      return opCode;
    } catch (ConfigException e) {
      String msg =
          String.format(
              "ReplicationFactorValidation requires you to define a replication factor "
                  + "comparison op in config '%s' with a supported operations code - %s.",
              TOPOLOGY_VALIDATIONS_REPLICATION_FACTOR_OP, Strings.join(validOpCodes, ','));
      throw new ConfigurationException(msg);
    }
  }

  private static short getReplicationFactor(Configuration config) throws ConfigurationException {
    try {
      return Short.parseShort(config.getProperty(TOPOLOGY_VALIDATIONS_REPLICATION_FACTOR_VALUE));
    } catch (ConfigException e) {
      String msg =
          String.format(
              "ReplicationFactorValidation requires you to define a replication factor value in config '%s'",
              TOPOLOGY_VALIDATIONS_REPLICATION_FACTOR_VALUE);
      throw new ConfigurationException(msg);
    }
  }
}
