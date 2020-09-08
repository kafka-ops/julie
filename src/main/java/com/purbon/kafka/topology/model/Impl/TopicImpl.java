package com.purbon.kafka.topology.model.Impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.TopicManager;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.TopicSchemas;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TopicImpl implements Topic, Cloneable {

  public static final String DEFAULT_TOPIC_NAME = "default";

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> dataType;

  @JsonInclude(Include.NON_EMPTY)
  private TopicSchemas schemas;

  private String name;

  private HashMap<String, String> config;

  private int partitionCount;
  private int replicationFactor;

  private String projectPrefix;

  public TopicImpl(String name) {
    this(name, Optional.empty(), new HashMap<>());
  }

  public TopicImpl(String name, String dataType) {
    this(name, Optional.of(dataType), new HashMap<>());
  }

  public TopicImpl(String name, Optional<String> dataType, HashMap<String, String> config) {
    this.name = name;
    this.dataType = dataType;
    this.config = config;
    this.replicationFactor = 0;
    this.partitionCount = 0;
  }

  public TopicImpl() {
    this(DEFAULT_TOPIC_NAME, Optional.empty(), new HashMap<>());
  }

  public String getName() {
    return name;
  }

  public TopicSchemas getSchemas() {
    return schemas;
  }

  public void setSchemas(TopicSchemas schemas) {
    this.schemas = schemas;
  }

  private String toString(String projectPrefix) {
    StringBuilder sb = new StringBuilder();
    sb.append(projectPrefix).append(".").append(getName());

    if (getDataType().isPresent()) {
      sb.append(".").append(getDataType().get());
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return toString(projectPrefix);
  }

  public void setName(String name) {
    this.name = name;
  }

  public HashMap<String, String> getConfig() {
    return config;
  }

  public void setConfig(HashMap<String, String> config) {
    this.config = config;
  }

  public Map<String, String> rawConfig() {
    String value = getConfig().remove(TopicManager.NUM_PARTITIONS);
    if (value != null) {
      partitionCount = Integer.valueOf(value);
    }
    value = getConfig().remove(TopicManager.REPLICATION_FACTOR);
    if (value != null) {
      replicationFactor = Integer.valueOf(value);
    }
    return getConfig();
  }

  public Optional<String> getDataType() {
    return dataType;
  }

  public void setProjectPrefix(String projectPrefix) {
    this.projectPrefix = projectPrefix;
  }

  public String getProjectPrefix() {
    return projectPrefix;
  }

  @Override
  public int partitionsCount() {
    String configValue = getConfig().get(TopicManager.NUM_PARTITIONS);
    if (configValue == null) {
      return partitionCount;
    } else {
      return Integer.valueOf(configValue);
    }
  }

  @Override
  public Topic clone() {
    try {
      return (Topic) super.clone();
    } catch (CloneNotSupportedException e) {
      return new TopicImpl(getName(), getDataType(), getConfig());
    }
  }
}
