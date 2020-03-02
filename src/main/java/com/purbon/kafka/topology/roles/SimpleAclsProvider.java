package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.ClusterState;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.kafka.common.acl.AclBinding;

public class SimpleAclsProvider implements AccessControlProvider {

  private final TopologyBuilderAdminClient adminClient;

  public SimpleAclsProvider(final TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void clearAcls(ClusterState clusterState) {

  }

  @Override
  public void setAclsForConnect(String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    adminClient
        .setAclsForConnect(principal, topicPrefix, readTopics, writeTopics);
  }

  @Override
  public void setAclsForStreamsApp(String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    adminClient
        .setAclsForStreamsApp(principal, topicPrefix, readTopics, writeTopics);
  }

  @Override
  public List<TopologyAclBinding> setAclsForConsumers(Collection<String> principals, String topic) {
    return principals
        .stream()
        .flatMap(principal -> {
          List<AclBinding> acls = adminClient.setAclsForConsumer(principal, topic);
          return acls
              .stream()
              .map(aclBinding -> new TopologyAclBinding(aclBinding));
        })
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> setAclsForProducers(Collection<String> principals, String topic) {
    return principals
        .stream()
        .flatMap(principal -> {
          List<AclBinding> acls = adminClient.setAclsForProducer(principal, topic);
          return acls
              .stream()
              .map(aclBinding -> new TopologyAclBinding(aclBinding));
        })
        .collect(Collectors.toList());

  }
}
