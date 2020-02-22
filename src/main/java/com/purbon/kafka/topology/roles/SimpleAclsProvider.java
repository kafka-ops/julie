package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import java.util.Collection;
import java.util.List;

public class SimpleAclsProvider implements AccessControlProvider {

  private final TopologyBuilderAdminClient adminClient;

  public SimpleAclsProvider(final TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void clearAcls() {
    adminClient.clearAcls();
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
  public void setAclsForConsumers(Collection<String> principals, String topic) {
    principals.forEach(principal -> adminClient.setAclsForConsumer(principal, topic));
  }

  @Override
  public void setAclsForProducers(Collection<String> principals, String topic) {
    principals.forEach(principal -> adminClient.setAclsForProducer(principal, topic));
  }
}
