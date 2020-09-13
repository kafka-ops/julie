package com.purbon.kafka.topology;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.Component;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AccessControlProvider {

  void createBindings(Set<TopologyAclBinding> bindings) throws IOException;

  void clearAcls(Set<TopologyAclBinding> bindings) throws IOException;

  List<TopologyAclBinding> setAclsForConnect(Connector connector, String topicPrefix);

  List<TopologyAclBinding> setAclsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics);

  List<TopologyAclBinding> setAclsForConsumers(Collection<Consumer> consumers, String topic);

  List<TopologyAclBinding> setAclsForProducers(Collection<String> principals, String topic);

  default TopologyAclBinding setPredefinedRole(
      String principal, String predefinedRole, String topicPrefix) {
    // NOOP
    return null;
  }

  Map<String, List<TopologyAclBinding>> listAcls();

  List<TopologyAclBinding> setAclsForSchemaRegistry(SchemaRegistryInstance schemaRegistry)
      throws ConfigurationException;

  List<TopologyAclBinding> setAclsForControlCenter(String principal, String appId);

  default List<TopologyAclBinding> setSchemaAuthorization(String principal, List<String> subjects) {
    return Collections.emptyList();
  }

  default List<TopologyAclBinding> setConnectorAuthorization(
      String principal, List<String> connectors) {
    return Collections.emptyList();
  }

  default List<TopologyAclBinding> setClusterLevelRole(
      String role, String principal, Component component) throws IOException {
    return Collections.emptyList();
  }
}
