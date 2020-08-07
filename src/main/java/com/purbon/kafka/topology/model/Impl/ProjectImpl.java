package com.purbon.kafka.topology.model.Impl;

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

  private String name;
  private List<String> zookeepers;

  private List<Consumer> consumers;
  private List<Producer> producers;
  private List<KStream> streams;
  private List<Connector> connectors;
  private List<Schemas> schemas;
  private Map<String, List<String>> rbacRawRoles;

  private List<Topic> topics;

  private String topologyPrefix;

  public ProjectImpl() {
    this("default");
  }

  public ProjectImpl(String name) {
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
    topic.setProjectPrefix(buildTopicPrefix());
    this.topics.add(topic);
  }

  public void setTopics(List<Topic> topics) {
    this.topics = topics;
  }

  public String buildTopicPrefix() {
    return buildTopicPrefix(topologyPrefix);
  }

  public String buildTopicPrefix(String topologyPrefix) {
    StringBuilder sb = new StringBuilder();
    sb.append(topologyPrefix).append(".").append(name);
    return sb.toString();
  }

  public void setTopologyPrefix(String topologyPrefix) {
    this.topologyPrefix = topologyPrefix;
  }

  public String getTopologyPrefix() {
    return topologyPrefix;
  }

  public void setRbacRawRoles(Map<String, List<String>> rbacRawRoles) {
    this.rbacRawRoles = rbacRawRoles;
  }

  public Map<String, List<String>> getRbacRawRoles() {
    return rbacRawRoles;
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
      project.setTopologyPrefix(getTopologyPrefix());
      project.setZookeepers(getZookeepers());
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
