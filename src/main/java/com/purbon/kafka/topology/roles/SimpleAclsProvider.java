package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.ClusterState;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.SchemaRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  public void clearAcls(ClusterState clusterState) {
    LOGGER.debug("AclsProvider: clearAcls");
    clusterState.forEachBinding(
        aclBinding -> {
          try {
            adminClient.clearAcls(aclBinding);
          } catch (IOException e) {
            LOGGER.error(e);
          }
        });
  }

  @Override
  public List<TopologyAclBinding> setAclsForConnect(Connector connector, String topicPrefix) {
    try {
      return adminClient.setAclsForConnect(connector).stream()
          .map(TopologyAclBinding::new)
          .collect(Collectors.toList());
    } catch (IOException e) {
      LOGGER.error(e);
      return new ArrayList<>();
    }
  }

  @Override
  public List<TopologyAclBinding> setAclsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    try {
      return adminClient.setAclsForStreamsApp(principal, topicPrefix, readTopics, writeTopics)
          .stream()
          .map(TopologyAclBinding::new)
          .collect(Collectors.toList());
    } catch (IOException e) {
      LOGGER.error(e);
      return new ArrayList<>();
    }
  }

  @Override
  public List<TopologyAclBinding> setAclsForConsumers(Collection<String> principals, String topic) {
    return principals.stream()
        .flatMap(
            principal -> {
              List<AclBinding> acls = new ArrayList<>();
              try {
                acls = adminClient.setAclsForConsumer(principal, topic);
              } catch (IOException e) {
                LOGGER.error(e);
              }
              return acls.stream().map(aclBinding -> new TopologyAclBinding(aclBinding));
            })
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> setAclsForProducers(Collection<String> principals, String topic) {
    return principals.stream()
        .flatMap(
            principal -> {
              List<AclBinding> acls = new ArrayList<>();
              try {
                acls = adminClient.setAclsForProducer(principal, topic);
              } catch (IOException e) {
                LOGGER.error(e);
              }
              return acls.stream().map(aclBinding -> new TopologyAclBinding(aclBinding));
            })
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> setAclsForSchemaRegistry(SchemaRegistry schemaRegistry) {
    try {
      return adminClient.setAclForSchemaRegistry(schemaRegistry).stream()
          .map(aclBinding -> new TopologyAclBinding(aclBinding))
          .collect(Collectors.toList());
    } catch (IOException e) {
      LOGGER.error(e);
      return new ArrayList<>();
    }
  }

  @Override
  public List<TopologyAclBinding> setAclsForControlCenter(String principal, String appId) {
    try {
      return adminClient.setAclsForControlCenter(principal, appId).stream()
          .map(aclBinding -> new TopologyAclBinding(aclBinding))
          .collect(Collectors.toList());
    } catch (IOException e) {
      LOGGER.error(e);
      return new ArrayList<>();
    }
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
                        .map(aclBinding -> new TopologyAclBinding(aclBinding))
                        .collect(Collectors.toList())));
    return map;
  }
}
