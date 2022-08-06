package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.model.schema.TopicSchemas;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.utils.JinjaUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Topic implements Cloneable {

  private static final String DEFAULT_TOPIC_NAME = "default";

  public static final String NUM_PARTITIONS = "num.partitions";
  public static final String REPLICATION_FACTOR = "replication.factor";

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> dataType;

  @JsonInclude(Include.NON_EMPTY)
  private List<TopicSchemas> schemas;

  private String name;
  private String dlqPrefix;

  private List<Producer> producers;
  private List<Consumer> consumers;

  @JsonInclude(Include.NON_EMPTY)
  private String plan;

  private Map<String, String> metadata;

  private Map<String, String> config;
  @JsonIgnore private Configuration appConfig;
  @JsonIgnore private Map<String, Object> context;

  private Optional<Integer> partitionCount;
  private Optional<Short> replicationFactor;

  @JsonIgnore private String projectPrefix;
  @JsonIgnore private String topicNamePattern;

  @JsonInclude(Include.NON_EMPTY)
  private Optional<SubjectNameStrategy> subjectNameStrategy;

  public Topic() {
    this(DEFAULT_TOPIC_NAME, Optional.empty(), new HashMap<>(), new Configuration());
  }

  public Topic(String name) {
    this(name, Optional.empty(), new HashMap<>(), new Configuration());
  }

  public Topic(String name, Map<String, String> config) {
    this(name, Optional.empty(), config, new Configuration());
  }

  public Topic(String name, Configuration config) {
    this(name, Optional.empty(), new HashMap<>(), config);
  }

  public Topic(String name, String dataType) {
    this(name, Optional.of(dataType), new HashMap<>(), new Configuration());
  }

  public Topic(String name, String dataType, Map<String, String> config) {
    this(name, Optional.of(dataType), config, new Configuration());
  }

  public Topic(String name, HashMap<String, String> config) {
    this(name, Optional.empty(), config, new Configuration());
  }

  public Topic(
      String name, Optional<String> dataType, Map<String, String> config, Configuration appConfig) {
    this(
        name,
        new ArrayList<>(),
        new ArrayList<>(),
        dataType,
        config,
        appConfig,
        appConfig.getTopicPrefixFormat());
  }

  public Topic(
      String name,
      List<Producer> producers,
      List<Consumer> consumers,
      Optional<String> dataType,
      Map<String, String> config,
      Configuration appConfig) {
    this(name, producers, consumers, dataType, config, appConfig, appConfig.getTopicPrefixFormat());
  }

  public Topic(
      String name,
      List<Producer> producers,
      List<Consumer> consumers,
      Optional<String> dataType,
      Map<String, String> config,
      Configuration appConfig,
      String topicNamePattern) {
    this.name = name;
    this.dlqPrefix = ""; // this topic is not a dlq topic
    this.producers = producers;
    this.consumers = consumers;
    this.dataType = dataType;
    this.config = config;
    this.schemas = new ArrayList<>();
    this.context = new HashMap<>();
    this.appConfig = appConfig;
    this.partitionCount = Optional.empty();
    this.replicationFactor = Optional.empty();
    if (config.containsKey(NUM_PARTITIONS)) {
      partitionCount = Optional.of(Integer.valueOf(config.get(NUM_PARTITIONS)));
    }
    if (config.containsKey(REPLICATION_FACTOR)) {
      replicationFactor = Optional.of(Short.valueOf(config.get(REPLICATION_FACTOR)));
    }
    subjectNameStrategy = Optional.empty();
    this.topicNamePattern = topicNamePattern;
  }

  public SubjectNameStrategy getSubjectNameStrategy() {
    return subjectNameStrategy.orElse(SubjectNameStrategy.TOPIC_NAME_STRATEGY);
  }

  private String toString(String projectPrefix) {
    switch (topicNamePattern) {
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
    if (dlqPrefix.isBlank()) {
      context.remove("dlq");
    } else {
      context.put("dlq", dlqPrefix);
    }
    dataType.ifPresentOrElse(s -> context.put("dataType", s), () -> context.remove("dataType"));
    return JinjaUtils.serialise(topicNamePattern, context);
  }

  private String defaultTopicStructureString(String projectPrefix) {
    StringBuilder sb = new StringBuilder();
    if (projectPrefix != null && !projectPrefix.isBlank()) {
      sb.append(projectPrefix);
    }
    sb.append(getName());

    if (getDataType().isPresent()) {
      sb.append(appConfig.getTopicPrefixSeparator()).append(getDataType().get());
    }

    if (!dlqPrefix.isBlank()) {
      sb.append(appConfig.getTopicPrefixSeparator()).append(dlqPrefix);
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return toString(projectPrefix);
  }

  @JsonIgnore
  public HashMap<String, String> getRawConfig() {
    HashMap<String, String> raw = new HashMap<>(config);
    raw.remove(REPLICATION_FACTOR);
    raw.remove(NUM_PARTITIONS);
    return raw;
  }

  public Optional<Short> replicationFactor() {
    return replicationFactor;
  }

  public void setConsumers(List<Consumer> consumers) {
    this.consumers.clear();
    this.consumers.addAll(consumers);
  }

  public void setProducers(List<Producer> producers) {
    this.producers.clear();
    this.producers.addAll(producers);
  }

  public Integer partitionsCount() {
    return partitionCount.orElse(-1);
  }

  @Override
  public Topic clone() {
    try {
      return (Topic) super.clone();
    } catch (CloneNotSupportedException e) {
      return new Topic(
          getName(),
          getProducers(),
          getConsumers(),
          getDataType(),
          getConfig(),
          getAppConfig(),
          topicNamePattern);
    }
  }
}
