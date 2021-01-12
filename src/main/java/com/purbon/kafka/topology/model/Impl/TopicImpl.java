package com.purbon.kafka.topology.model.Impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.TopicManager;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.SubjectNameStrategy;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.TopicSchemas;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.utils.JinjaUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TopicImpl implements Topic, Cloneable {

  private static final String DEFAULT_TOPIC_NAME = "default";

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> dataType;

  @JsonInclude(Include.NON_EMPTY)
  private List<TopicSchemas> schemas;

  private String name;

  private List<Producer> producers;
  private List<Consumer> consumers;

  @JsonInclude(Include.NON_EMPTY)
  private String plan;

  private Map<String, String> config;
  @JsonIgnore private TopologyBuilderConfig appConfig;
  @JsonIgnore private Map<String, Object> context;

  private int partitionCount;
  private short replicationFactor;

  @JsonIgnore private String projectPrefix;
  private static String DEFAULT_PARTITION_COUNT = "3";
  private static String DEFAULT_REPLICATION_FACTOR = "2";

  @JsonInclude(Include.NON_EMPTY)
  private Optional<SubjectNameStrategy> subjectNameStrategy;

  public TopicImpl() {
    this(DEFAULT_TOPIC_NAME, Optional.empty(), new HashMap<>(), new TopologyBuilderConfig());
  }

  public TopicImpl(String name) {
    this(name, Optional.empty(), new HashMap<>(), new TopologyBuilderConfig());
  }

  public TopicImpl(String name, TopologyBuilderConfig config) {
    this(name, Optional.empty(), new HashMap<>(), config);
  }

  public TopicImpl(String name, String dataType) {
    this(name, Optional.of(dataType), new HashMap<>(), new TopologyBuilderConfig());
  }

  public TopicImpl(String name, String dataType, HashMap<String, String> config) {
    this(name, Optional.of(dataType), config, new TopologyBuilderConfig());
  }

  public TopicImpl(String name, HashMap<String, String> config) {
    this(name, Optional.empty(), config, new TopologyBuilderConfig());
  }

  public TopicImpl(
      String name,
      Optional<String> dataType,
      Map<String, String> config,
      TopologyBuilderConfig appConfig) {
    this(name, new ArrayList<>(), new ArrayList<>(), dataType, config, appConfig);
  }

  public TopicImpl(
      String name,
      List<Producer> producers,
      List<Consumer> consumers,
      Optional<String> dataType,
      Map<String, String> config,
      TopologyBuilderConfig appConfig) {
    this.name = name;
    this.producers = producers;
    this.consumers = consumers;
    this.dataType = dataType;
    this.config = config;
    this.schemas = new ArrayList<>();
    this.context = new HashMap<>();
    this.replicationFactor = 0;
    this.partitionCount = 0;
    this.appConfig = appConfig;
    String value = config.getOrDefault(TopicManager.NUM_PARTITIONS, DEFAULT_PARTITION_COUNT);
    partitionCount = Integer.parseInt(value);
    value = config.getOrDefault(TopicManager.REPLICATION_FACTOR, DEFAULT_REPLICATION_FACTOR);
    replicationFactor = Short.parseShort(value);
    subjectNameStrategy = Optional.empty();
  }

  public String getName() {
    return name;
  }

  @Override
  public String getPlan() {
    return plan;
  }

  public List<TopicSchemas> getSchemas() {
    return schemas;
  }

  public void setSchemas(List<TopicSchemas> schemas) {
    this.schemas = schemas;
  }

  public void setSubjectNameStrategy(Optional<SubjectNameStrategy> subjectNameStrategy) {
    this.subjectNameStrategy = subjectNameStrategy;
  }

  public SubjectNameStrategy getSubjectNameStrategy() {
    return subjectNameStrategy.orElse(SubjectNameStrategy.TOPIC_NAME_STRATEGY);
  }

  private String toString(String projectPrefix) {
    switch (appConfig.getTopicPrefixFormat()) {
      case "default":
        return defaultTopicStructureString(projectPrefix);
      case "name":
        return name;
      default:
        return patternBasedTopicNameStructureString();
    }
  }

  private String patternBasedTopicNameStructureString() {
    context.put("topic", name);
    dataType.ifPresent(s -> context.put("dataType", s));
    return JinjaUtils.serialise(appConfig.getTopicPrefixFormat(), context);
  }

  private String defaultTopicStructureString(String projectPrefix) {
    StringBuilder sb = new StringBuilder();
    sb.append(projectPrefix).append(appConfig.getTopicPrefixSeparator()).append(getName());

    if (getDataType().isPresent()) {
      sb.append(appConfig.getTopicPrefixSeparator()).append(getDataType().get());
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return toString(projectPrefix);
  }

  @Override
  public Map<String, String> getConfig() {
    return config;
  }

  @JsonIgnore
  @Override
  public HashMap<String, String> getRawConfig() {
    HashMap<String, String> raw = new HashMap<>(config);
    raw.remove(TopicManager.REPLICATION_FACTOR);
    raw.remove(TopicManager.NUM_PARTITIONS);
    return raw;
  }

  public Optional<String> getDataType() {
    return dataType;
  }

  public void setDefaultProjectPrefix(String projectPrefix) {
    this.projectPrefix = projectPrefix;
  }

  @Override
  public void setPrefixContext(Map<String, Object> properties) {
    this.context = properties;
  }

  @Override
  public short replicationFactor() {
    return replicationFactor;
  }

  @Override
  public List<Consumer> getConsumers() {
    return consumers;
  }

  @Override
  public List<Producer> getProducers() {
    return producers;
  }

  @Override
  public void setConsumers(List<Consumer> consumers) {
    this.consumers.clear();
    this.consumers.addAll(consumers);
  }

  @Override
  public void setProducers(List<Producer> producers) {
    this.producers.clear();
    this.producers.addAll(producers);
  }

  @Override
  public int partitionsCount() {
    return partitionCount;
  }

  public TopologyBuilderConfig getAppConfig() {
    return appConfig;
  }

  public void addAppConfig(TopologyBuilderConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  public Topic clone() {
    try {
      return (Topic) super.clone();
    } catch (CloneNotSupportedException e) {
      return new TopicImpl(
          getName(), getProducers(), getConsumers(), getDataType(), getConfig(), getAppConfig());
    }
  }
}
