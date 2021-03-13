package com.purbon.kafka.topology.model.Impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.model.PlatformSystem;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.utils.JinjaUtils;
import java.util.*;

public class ProjectImpl implements Project, Cloneable {

  @JsonIgnore private Configuration config;

  private String name;
  private List<String> zookeepers;

  private PlatformSystem<Consumer> consumers;
  private PlatformSystem<Producer> producers;
  private PlatformSystem<KStream> streams;
  private PlatformSystem<Connector> connectors;
  private PlatformSystem<Schemas> schemas;
  private Map<String, List<String>> rbacRawRoles;

  private List<Topic> topics;

  @JsonIgnore private List<String> order;
  @JsonIgnore private Map<String, Object> prefixContext;

  public ProjectImpl() {
    this("default");
  }

  public ProjectImpl(String name) {
    this(name, new Configuration());
  }

  public ProjectImpl(String name, Configuration config) {
    this(
        name,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        new HashMap<>(),
        config);
  }

  public ProjectImpl(
      String name,
      Optional<PlatformSystem<Consumer>> consumers,
      Optional<PlatformSystem<Producer>> producers,
      Optional<PlatformSystem<KStream>> streams,
      Optional<PlatformSystem<Connector>> connectors,
      Optional<PlatformSystem<Schemas>> schemas,
      Map<String, List<String>> rbacRawRoles,
      Configuration config) {
    this(
        name,
        new ArrayList<>(),
        consumers.orElse(new PlatformSystem<>()),
        producers.orElse(new PlatformSystem<>()),
        streams.orElse(new PlatformSystem<>()),
        new ArrayList<>(),
        connectors.orElse(new PlatformSystem<>()),
        schemas.orElse(new PlatformSystem<>()),
        rbacRawRoles,
        config);
  }

  public ProjectImpl(
      String name,
      List<Topic> topics,
      PlatformSystem<Consumer> consumers,
      PlatformSystem<Producer> producers,
      PlatformSystem<KStream> streams,
      List<String> zookeepers,
      PlatformSystem<Connector> connectors,
      PlatformSystem<Schemas> schemas,
      Map<String, List<String>> rbacRawRoles,
      Configuration config) {
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
    return consumers.getAccessControlLists();
  }

  public void setConsumers(List<Consumer> consumers) {
    this.consumers = new PlatformSystem<>(consumers);
  }

  public List<Producer> getProducers() {
    return producers.getAccessControlLists();
  }

  public void setProducers(List<Producer> producers) {
    this.producers = new PlatformSystem<>(producers);
  }

  public List<KStream> getStreams() {
    return streams.getAccessControlLists();
  }

  public void setStreams(List<KStream> streams) {
    this.streams = new PlatformSystem<>(streams);
  }

  public List<Connector> getConnectors() {
    return connectors.getAccessControlLists();
  }

  public List<KafkaConnectArtefact> getConnectorArtefacts() {
    return (List<KafkaConnectArtefact>) connectors.getArtefacts();
  }

  public void setConnectors(List<Connector> connectors) {
    this.connectors = new PlatformSystem<>(connectors);
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
              new PlatformSystem<>(getConsumers()),
              new PlatformSystem<>(getProducers()),
              new PlatformSystem<>(getStreams()),
              getZookeepers(),
              new PlatformSystem<>(getConnectors()),
              new PlatformSystem<>(getSchemas()),
              getRbacRawRoles(),
              config);
      project.setPrefixContextAndOrder(prefixContext, order);
      return project;
    }
  }

  @Override
  public List<Schemas> getSchemas() {
    return schemas.getAccessControlLists();
  }

  @Override
  public void setSchemas(List<Schemas> schemas) {
    this.schemas = new PlatformSystem<>(schemas);
  }
}
