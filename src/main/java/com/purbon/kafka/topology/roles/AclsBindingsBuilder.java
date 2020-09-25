package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AclsBindingsBuilder implements BindingsBuilderProvider {

  private final TopologyBuilderAdminClient adminClient;

  public AclsBindingsBuilder(final TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForConnect(Connector connector, String topicPrefix) {
    return adminClient.setAclsForConnect(connector).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    return adminClient.setAclsForStreamsApp(principal, topicPrefix, readTopics, writeTopics)
        .stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForConsumers(
      Collection<Consumer> consumers, String topic) {
    return consumers.stream()
        .flatMap(
            consumer ->
                adminClient.setAclsForConsumer(consumer, topic).stream()
                    .map(TopologyAclBinding::new))
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForProducers(
      Collection<String> principals, String topic) {
    return principals.stream()
        .flatMap(
            principal ->
                adminClient.setAclsForProducer(principal, topic).stream()
                    .map(TopologyAclBinding::new))
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForSchemaRegistry(
      SchemaRegistryInstance schemaRegistry) {
    return adminClient.setAclForSchemaRegistry(schemaRegistry).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForControlCenter(String principal, String appId) {
    return adminClient.setAclsForControlCenter(principal, appId).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }
}
