package com.purbon.kafka.topology.model;

import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import java.util.List;

public class Project {

  private String name;
  private List<String> zookeepers;

  private List<Consumer> consumers;
  private List<Producer> producers;
  private List<KStream> streams;
  private List<Connector> connectors;

  private List<Topic> topics;

  public Project() {

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

  public void setTopics(List<Topic> topics) {
    this.topics = topics;
  }
}
