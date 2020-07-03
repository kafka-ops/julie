package com.purbon.kafka.topology;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.users.SchemaRegistry;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AccessControlProvider {

  void clearAcls(ClusterState clusterState);

  List<TopologyAclBinding> setAclsForConnect(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics)
      throws IOException;

  List<TopologyAclBinding> setAclsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics);

  List<TopologyAclBinding> setAclsForConsumers(Collection<String> principals, String topic);

  List<TopologyAclBinding> setAclsForProducers(Collection<String> principals, String topic);

  default TopologyAclBinding setPredefinedRole(
      String principal, String predefinedRole, String topicPrefix) {
    // NOOP
    return null;
  }

  Map<String, List<TopologyAclBinding>> listAcls();

  List<TopologyAclBinding> setAclsForSchemaRegistry(SchemaRegistry schemaRegistry)
      throws ConfigurationException;

  List<TopologyAclBinding> setAclsForControlCenter(String principal, String appId);
}
