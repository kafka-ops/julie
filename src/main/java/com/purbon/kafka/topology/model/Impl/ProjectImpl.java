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
    this("default", new TopologyBuilderConfig());
  }

  public ProjectImpl(String name) {
    this(name, new TopologyBuilderConfig());
  }

  public ProjectImpl(String name, TopologyBuilderConfig config) {
    this.name = name;
    this.topics = new ArrayList<>();
    this.consumers = new ArrayList<>();
    this.producers = new ArrayList<>();
    this.streams = new ArrayList<>();
    this.consumers = new ArrayList<>();
    this.zookeepers = new ArrayList<>();
    this.connectors = new ArrayList<>();
    this.schemas = new ArrayList<>();
    this.rbacRawRoles = new HashMap<>();
    this.config = config;
    this.prefixContext = new HashMap<>();
    this.order = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getZookeepers() {
    return zookeepers;
  }

  public void setZookeepers(List<String> zookeepers) {
    this.zookeepers = zookeepers;
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
    topics.forEach(t -> addTopic(t));
  }

  public String namePrefix() {
    return namePrefix(buildNamePrefix());
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
  public void addConfig(TopologyBuilderConfig config) {
    this.config = config;
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
      ProjectImpl project = new ProjectImpl();
      project.setConnectors(getConnectors());
      project.setConsumers(getConsumers());
      project.setName(getName());
      project.setRbacRawRoles(getRbacRawRoles());
      project.setProducers(getProducers());
      project.setStreams(getStreams());
      project.setTopics(getTopics());
      project.setZookeepers(getZookeepers());
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
