package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.purbon.kafka.topology.model.impl.ProjectImpl;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import java.util.List;
import java.util.Map;

/**
 * Project model representation.
 *
 * YAML:
 * <pre>
 * {@code
 * projects:
 *   - name: "projectA"
 *     consumers:
 *       - principal: "User:App0"
 *       - principal: "User:App1"
 *     producers:
 *       - principal: "User:App3"
 *       - principal: "User:App4"
 *     streams:
 *       - principal: "User:Streams0"
 *         topics:
 *           read:
 *             - "topicA"
 *             - "topicB"
 *           write:
 *             - "topicC"
 *             - "topicD"
 *     connectors:
 *       - principal: "User:Connect1"
 *         group: "group"
 *         status_topic: "status"
 *         offset_topic: "offset"
 *         configs_topic: "configs"
 *         topics:
 *           read:
 *             - "topicA"
 *             - "topicB"
 *       - principal: "User:Connect2"
 *         topics:
 *           write:
 *             - "topicC"
 *             - "topicD"
 * }
 * </pre>
 */
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

  List<Connector> getConnectors();

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
