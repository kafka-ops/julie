package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import java.util.List;
import java.util.Map;

@JsonDeserialize(as = ProjectImpl.class)
public interface Project {

  String getName();

  void setName(String name);

  List<String> getZookeepers();

  void setZookeepers(List<String> zookeepers);

  List<Consumer> getConsumers();

  void setConsumers(List<Consumer> consumers);

  List<Producer> getProducers();

  void setProducers(List<Producer> producers);

  List<KStream> getStreams();

  void setStreams(List<KStream> streams);

  List<Connector> getConnectors();

  void setConnectors(List<Connector> connectors);

  List<Schemas> getSchemas();

  void setSchemas(List<Schemas> schemas);

  List<Topic> getTopics();

  void addTopic(Topic topic);

  void setTopics(List<Topic> topics);

  String buildTopicPrefix();

  String buildTopicPrefix(String topologyPrefix);

  void setTopologyPrefix(String topologyPrefix);

  String getTopologyPrefix();

  void setRbacRawRoles(Map<String, List<String>> rbacRawRoles);

  Map<String, List<String>> getRbacRawRoles();
}
