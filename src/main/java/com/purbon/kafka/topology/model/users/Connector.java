package com.purbon.kafka.topology.model.users;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.model.DynamicUser;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class Connector extends DynamicUser {

  private static final String DEFAULT_CONNECT_STATUS_TOPIC = "connect-status";
  private static final String DEFAULT_CONNECT_OFFSET_TOPIC = "connect-offsets";
  private static final String DEFAULT_CONNECT_CONFIGS_TOPIC = "connect-configs";
  private static final String DEFAULT_CONNECT_GROUP = "connect-cluster";

  private static final String DEFAULT_CONNECT_CLUSTER_ID = "connect-cluster";

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> status_topic;

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> offset_topic;

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> configs_topic;

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> group;

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> cluster_id;

  private Optional<List<String>> connectors;

  public Connector() {
    this("");
  }

  public Connector(String principal) {
    this(
        principal,
        new HashMap<>(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  public Connector(
      String principal,
      HashMap<String, List<String>> topics,
      Optional<String> status_topic,
      Optional<String> offset_topic,
      Optional<String> configs_topic,
      Optional<String> group,
      Optional<String> cluster_id,
      Optional<List<String>> connectors) {

    super(principal, topics);

    this.configs_topic = configs_topic;
    this.status_topic = status_topic;
    this.offset_topic = offset_topic;
    this.group = group;
    this.cluster_id = cluster_id;
    this.connectors = connectors;
  }

  public String statusTopicString() {
    return status_topic.orElse(DEFAULT_CONNECT_STATUS_TOPIC);
  }

  public String offsetTopicString() {
    return offset_topic.orElse(DEFAULT_CONNECT_OFFSET_TOPIC);
  }

  public String configsTopicString() {
    return configs_topic.orElse(DEFAULT_CONNECT_CONFIGS_TOPIC);
  }

  public String groupString() {
    return group.orElse(DEFAULT_CONNECT_GROUP);
  }

  public void setStatus_topic(Optional<String> status_topic) {
    this.status_topic = status_topic;
  }

  public void setOffset_topic(Optional<String> offset_topic) {
    this.offset_topic = offset_topic;
  }

  public void setConfigs_topic(Optional<String> configs_topic) {
    this.configs_topic = configs_topic;
  }

  public void setGroup(Optional<String> group) {
    this.group = group;
  }

  public void setCluster_id(Optional<String> cluster_id) {
    this.cluster_id = cluster_id;
  }

  public Optional<String> getCluster_id() {
    return cluster_id;
  }

  public Optional<String> getStatus_topic() {
    return status_topic;
  }

  public Optional<String> getOffset_topic() {
    return offset_topic;
  }

  public Optional<String> getConfigs_topic() {
    return configs_topic;
  }

  public Optional<String> getGroup() {
    return group;
  }

  public Optional<List<String>> getConnectors() {
    return connectors;
  }

  public void setConnectors(Optional<List<String>> connectors) {
    this.connectors = connectors;
  }
}
