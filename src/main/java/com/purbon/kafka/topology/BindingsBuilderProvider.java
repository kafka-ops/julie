package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Component;
import com.purbon.kafka.topology.model.JulieRoleAcl;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KSqlApp;
import com.purbon.kafka.topology.model.users.Other;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.KsqlServerInstance;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface BindingsBuilderProvider {

  List<TopologyAclBinding> buildBindingsForConnect(Connector connector, String topicPrefix);

  List<TopologyAclBinding> buildBindingsForStreamsApp(
      String principal,
      String topicPrefix,
      List<String> readTopics,
      List<String> writeTopics,
      boolean eos);

  List<TopologyAclBinding> buildBindingsForConsumers(
      Collection<Consumer> consumers, String resource, boolean prefixed);

  List<TopologyAclBinding> buildBindingsForProducers(
      Collection<Producer> principals, String resource, boolean prefixed);

  default TopologyAclBinding setPredefinedRole(
      String principal, String predefinedRole, String topicPrefix) {
    // NOOP
    return null;
  }

  List<TopologyAclBinding> buildBindingsForSchemaRegistry(SchemaRegistryInstance schemaRegistry);

  List<TopologyAclBinding> buildBindingsForControlCenter(String principal, String appId);

  default List<TopologyAclBinding> setSchemaAuthorization(
      String principal,
      List<String> subjects,
      String role,
      boolean prefixed,
      Boolean shouldOptimizeAcls,
      String namePrefix) {
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

  Collection<TopologyAclBinding> buildBindingsForKSqlServer(KsqlServerInstance ksqlServer);

  Collection<TopologyAclBinding> buildBindingsForKSqlApp(KSqlApp app, String prefix);

  Collection<TopologyAclBinding> buildBindingsForJulieRole(
      Other other, String name, List<JulieRoleAcl> acls) throws IOException;
}
