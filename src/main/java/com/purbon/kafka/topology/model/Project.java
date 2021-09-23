package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.artefact.KConnectArtefacts;
import com.purbon.kafka.topology.model.artefact.KsqlArtefacts;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KSqlApp;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Other;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import java.util.List;
import java.util.Map;

@JsonDeserialize(as = ProjectImpl.class)
public interface Project {

  String getName();

  List<String> getZookeepers();

  List<Consumer> getConsumers();

  void setConsumers(List<Consumer> consumers);

  List<Producer> getProducers();

  void setProducers(List<Producer> producers);

  List<KStream> getStreams();

  void setStreams(List<KStream> streams);

  List<KSqlApp> getKSqls();

  void setKSqls(List<KSqlApp> ksqls);

  Map<String, List<Other>> getOthers();

  List<Connector> getConnectors();

  KConnectArtefacts getConnectorArtefacts();

  KsqlArtefacts getKsqlArtefacts();

  void setConnectors(List<Connector> connectors);

  List<Schemas> getSchemas();

  void setSchemas(List<Schemas> schemas);

  List<Topic> getTopics();

  void addTopic(Topic topic);

  void setTopics(List<Topic> topics);

  String namePrefix();

  void setRbacRawRoles(Map<String, List<String>> rbacRawRoles);

  Map<String, List<String>> getRbacRawRoles();

  void setPrefixContextAndOrder(Map<String, Object> asFullContext, List<String> order);
}
