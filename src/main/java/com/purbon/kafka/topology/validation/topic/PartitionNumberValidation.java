package com.purbon.kafka.topology.validation.topic;

import static com.purbon.kafka.topology.Constants.TOPOLOGY_VALIDATIONS_PARTITION_NUMBER_OP;
import static com.purbon.kafka.topology.Constants.TOPOLOGY_VALIDATIONS_PARTITION_NUMBER_VALUE;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.TopicValidation;
import com.typesafe.config.ConfigException;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class PartitionNumberValidation implements TopicValidation {

  private final int partitionNumberValue;
  private final String partitionNumberOp;
  private Configuration config;

  public PartitionNumberValidation(Configuration config) throws ConfigurationException {
    this(getPartitionNumber(config), getPartitionNumberOp(config));
    this.config = config;
  }

  public PartitionNumberValidation(int partitionNumberValue, String partitionNumberOp) {
    this.partitionNumberValue = partitionNumberValue;
    this.partitionNumberOp = partitionNumberOp;
  }

  @Override
  public void valid(Topic topic) throws ValidationException {
    if (topic.getPartitionCount().isPresent() && !validatePartitionCount(topic.partitionsCount())) {
      String msg =
          String.format(
              "Topic %s has an invalid number of partitions: %s", topic, topic.partitionsCount());
      throw new ValidationException(msg);
    }
  }

  private boolean validatePartitionCount(int topicValue) throws ValidationException {
    boolean result;
    switch (partitionNumberOp) {
      case "gt":
        result = topicValue > partitionNumberValue;
        break;
      case "lt":
        result = topicValue < partitionNumberValue;
        break;
      case "eq":
        result = topicValue == partitionNumberValue;
        break;
      case "gte":
        result = topicValue >= partitionNumberValue;
        break;
      case "lte":
        result = topicValue <= partitionNumberValue;
        break;
      case "ne":
        result = topicValue != partitionNumberValue;
        break;
      default:
        throw new ValidationException("Invalid Operation code in use " + partitionNumberOp);
    }
    return result;
  }

  private static String getPartitionNumberOp(Configuration config) throws ConfigurationException {
    List<String> validOpCodes = Arrays.asList("gt", "lt", "eq", "gte", "lte", "ne");
    try {
      String opCode =
          config.getProperty(TOPOLOGY_VALIDATIONS_PARTITION_NUMBER_OP).toLowerCase().strip();
      if (!validOpCodes.contains(opCode)) {
        throw new ConfigException.BadValue(
            TOPOLOGY_VALIDATIONS_PARTITION_NUMBER_OP, "Not supported operation value");
      }
      return opCode;
    } catch (ConfigException e) {
      String msg =
          String.format(
              "PartitionNumberValidation requires you to define a partition number "
                  + "comparison op in config '%s' with a supported operations code - %s.",
              TOPOLOGY_VALIDATIONS_PARTITION_NUMBER_OP, Strings.join(validOpCodes, ','));
      throw new ConfigurationException(msg);
    }
  }

  private static int getPartitionNumber(Configuration config) throws ConfigurationException {
    try {
      return Integer.parseInt(config.getProperty(TOPOLOGY_VALIDATIONS_PARTITION_NUMBER_VALUE));
    } catch (ConfigException e) {
      String msg =
          String.format(
              "PartitionNumberValidation requires you to define a partition number value in config '%s'",
              TOPOLOGY_VALIDATIONS_PARTITION_NUMBER_VALUE);
      throw new ConfigurationException(msg);
    }
  }
}
