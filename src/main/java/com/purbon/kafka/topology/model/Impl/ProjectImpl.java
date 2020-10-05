package com.purbon.kafka.topology.model.Impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.utils.JinjaUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectImpl implements Project, Cloneable {

  @JsonIgnore private TopologyBuilderConfig config;

  private String name;
  private List<String> zookeepers;

  private List<Consumer> consumers;
  private List<Producer> producers;
  private List<KStream> streams;
  private List<Connector> connectors;
  private List<Schemas> schemas;
  private Map<String, List<String>> rbacRawRoles;

  private List<Topic> topics;

  @JsonIgnore private List<String> order;
  @JsonIgnore private Map<String, Object> prefixContext;

  public ProjectImpl() {
    this("default");
  }

  public ProjectImpl(String name) {
    this(name, new TopologyBuilderConfig());
  }

  public ProjectImpl(String name, TopologyBuilderConfig config) {
    this(
        name,
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        new HashMap<>(),
        config);
  }

  public ProjectImpl(
      String name,
      List<Consumer> consumers,
      List<Producer> producers,
      List<KStream> streams,
      List<Connector> connectors,
      List<Schemas> schemas,
      Map<String, List<String>> rbacRawRoles,
      TopologyBuilderConfig config) {
    this(
        name,
        new ArrayList<>(),
        consumers,
        producers,
        streams,
        new ArrayList<>(),
        connectors,
        schemas,
        rbacRawRoles,
        config);
  }

  public ProjectImpl(
      String name,
      List<Topic> topics,
      List<Consumer> consumers,
      List<Producer> producers,
      List<KStream> streams,
      List<String> zookeepers,
      List<Connector> connectors,
      List<Schemas> schemas,
      Map<String, List<String>> rbacRawRoles,
      TopologyBuilderConfig config) {
    this.name = name;
    this.topics = topics;
    this.consumers = consumers;
    this.producers = producers;
    this.streams = streams;
    this.zookeepers = zookeepers;
    this.connectors = connectors;
    this.schemas = schemas;
    this.rbacRawRoles = rbacRawRoles;
    this.config = config;
    this.prefixContext = new HashMap<>();
    this.order = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public List<String> getZookeepers() {
    return zookeepers;
  }

  public List<Consumer> getConsumers() {
    return consumers;
  }

  public void setConsumers(List<Consumer> consumers) {
    this.consumers = consumers;
  }

  public List<Producer> getProducers() {
    return producers;
  }

  public void setProducers(List<Producer> producers) {
    this.producers = producers;
  }

  public List<KStream> getStreams() {
    return streams;
  }

  public void setStreams(List<KStream> streams) {
    this.streams = streams;
  }

  public List<Connector> getConnectors() {
    return connectors;
  }

  public void setConnectors(List<Connector> connectors) {
    this.connectors = connectors;
  }

  public List<Topic> getTopics() {
    return topics;
  }

  public void addTopic(Topic topic) {
    topic.setDefaultProjectPrefix(namePrefix());
    prefixContext.put("project", getName());
    topic.setPrefixContext(prefixContext);
    this.topics.add(topic);
  }

  public void setTopics(List<Topic> topics) {
    this.topics.clear();
    topics.forEach(this::addTopic);
  }

  public String namePrefix() {
    if (config.getProjectPrefixFormat().equals("default")) return namePrefix(buildNamePrefix());
    else return patternBasedProjectPrefix();
  }

  private String patternBasedProjectPrefix() {
    return JinjaUtils.serialise(config.getProjectPrefixFormat(), prefixContext);
  }

  private String namePrefix(String topologyPrefix) {
    StringBuilder sb = new StringBuilder();
    sb.append(topologyPrefix).append(config.getTopicPrefixSeparator()).append(name);
    return sb.toString();
  }

  private String buildNamePrefix() {
    StringBuilder sb = new StringBuilder();
    sb.append(prefixContext.get("context"));
    for (String key : order) {
      sb.append(config.getTopicPrefixSeparator());
      sb.append(prefixContext.get(key));
    }
    return sb.toString();
  }

  public void setRbacRawRoles(Map<String, List<String>> rbacRawRoles) {
    this.rbacRawRoles = rbacRawRoles;
  }

  public Map<String, List<String>> getRbacRawRoles() {
    return rbacRawRoles;
  }

  @Override
  public void setPrefixContextAndOrder(Map<String, Object> prefixContext, List<String> order) {
    this.prefixContext = prefixContext;
    this.prefixContext.put("project", getName());
    this.order = order;
  }

  @Override
  public ProjectImpl clone() {
    try {
      return (ProjectImpl) super.clone();
    } catch (CloneNotSupportedException e) {
      ProjectImpl project =
          new ProjectImpl(
              getName(),
              getTopics(),
              getConsumers(),
              getProducers(),
              getStreams(),
              getZookeepers(),
              getConnectors(),
              getSchemas(),
              getRbacRawRoles(),
              config);
      project.setPrefixContextAndOrder(prefixContext, order);
      return project;
    }
  }

  @Override
  public List<Schemas> getSchemas() {
    return schemas;
  }

  @Override
  public void setSchemas(List<Schemas> schemas) {
    this.schemas = schemas;
  }
}
