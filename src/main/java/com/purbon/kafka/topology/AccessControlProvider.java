package com.purbon.kafka.topology;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AccessControlProvider {

  void clearAcls(ClusterState clusterState);

  List<TopologyAclBinding> setAclsForConnect(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics);

  List<TopologyAclBinding> setAclsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics);

  List<TopologyAclBinding> setAclsForConsumers(Collection<String> principals, String topic);

  List<TopologyAclBinding> setAclsForProducers(Collection<String> principals, String topic);

  default void setPredefinedRole(String principal, String predefinedRole, String topicPrefix) {
    // NOOP
  }

  Map<String, List<TopologyAclBinding>> listAcls();

  default List<TopologyAclBinding> setAclsForSchemaRegistry(String principal) {
    // NOOP
    return new ArrayList<>();
  }
}
