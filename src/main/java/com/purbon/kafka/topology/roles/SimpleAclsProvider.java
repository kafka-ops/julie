package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleAclsProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(SimpleAclsProvider.class);

  private final TopologyBuilderAdminClient adminClient;

  public SimpleAclsProvider(final TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void createBindings(Set<TopologyAclBinding> bindings) throws IOException {
    LOGGER.debug("AclsProvider: createBindings");
    List<AclBinding> bindingsAsNativeKafka =
        bindings.stream()
            .filter(binding -> binding.asAclBinding().isPresent())
            .map(binding -> binding.asAclBinding().get())
            .collect(Collectors.toList());
    try {
      adminClient.createAcls(bindingsAsNativeKafka);
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public void clearAcls(Set<TopologyAclBinding> bindings) throws IOException {
    LOGGER.debug("AclsProvider: clearAcls");
    for (TopologyAclBinding binding : bindings) {
      try {
        adminClient.clearAcls(binding);
      } catch (IOException ex) {
        LOGGER.error(ex);
        throw ex;
      }
    }
  }

  @Override
  public List<TopologyAclBinding> setAclsForConnect(Connector connector, String topicPrefix) {
    return adminClient.setAclsForConnect(connector).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> setAclsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    return adminClient.setAclsForStreamsApp(principal, topicPrefix, readTopics, writeTopics)
        .stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> setAclsForConsumers(
      Collection<Consumer> consumers, String topic) {
    return consumers.stream()
        .flatMap(
            consumer ->
                adminClient.setAclsForConsumer(consumer, topic).stream()
                    .map(TopologyAclBinding::new))
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> setAclsForProducers(Collection<String> principals, String topic) {
    return principals.stream()
        .flatMap(
            principal ->
                adminClient.setAclsForProducer(principal, topic).stream()
                    .map(TopologyAclBinding::new))
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> setAclsForSchemaRegistry(SchemaRegistryInstance schemaRegistry) {
    return adminClient.setAclForSchemaRegistry(schemaRegistry).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> setAclsForControlCenter(String principal, String appId) {
    return adminClient.setAclsForControlCenter(principal, appId).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public Map<String, List<TopologyAclBinding>> listAcls() {
    Map<String, List<TopologyAclBinding>> map = new HashMap<>();
    adminClient
        .fetchAclsList()
        .forEach(
            (topic, aclBindings) ->
                map.put(
                    topic,
                    aclBindings.stream()
                        .map(TopologyAclBinding::new)
                        .collect(Collectors.toList())));
    return map;
  }
}
