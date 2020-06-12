package com.purbon.kafka.topology.roles;

import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_WRITE;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.RESOURCE_OWNER;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.SECURITY_ADMIN;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.ClusterState;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RBACProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(RBACProvider.class);

  public static final String LITERAL = "LITERAL";
  public static final String PREFIX = "PREFIXED";
  private final MDSApiClient apiClient;

  public RBACProvider(MDSApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public void clearAcls(ClusterState clusterState) {}

  @Override
  public List<TopologyAclBinding> setAclsForConnect(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {

    apiClient.bind(principal, DEVELOPER_READ, topicPrefix, PREFIX);
    if (readTopics != null && readTopics.isEmpty()) {
      readTopics.forEach(topic -> apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL));
    }
    if (writeTopics != null && readTopics.isEmpty()) {
      writeTopics.forEach(topic -> apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL));
    }

    String[] resources =
        new String[] {
          "Topic:connect-configs",
          "Topic:connect-offsets",
          "Topic:connect-status",
          "Group:connect-cluster",
          "Group:secret-registry",
          "Topic:_confluent-secrets"
        };

    Arrays.asList(resources)
        .forEach(
            resourceObject -> {
              String[] elements = resourceObject.split(":");
              String resource = elements[0];
              String resourceType = elements[1];
              apiClient.bind(principal, RESOURCE_OWNER, resource, resourceType, LITERAL);
            });
    return new ArrayList<>();
  }

  @Override
  public List<TopologyAclBinding> setAclsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {

    apiClient.bind(principal, DEVELOPER_READ, topicPrefix, PREFIX);
    readTopics.forEach(topic -> apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL));
    writeTopics.forEach(topic -> apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL));

    apiClient.bind(principal, RESOURCE_OWNER, topicPrefix, PREFIX);
    apiClient.bind(principal, RESOURCE_OWNER, topicPrefix, "Group", PREFIX);

    return new ArrayList<>();
  }

  @Override
  public List<TopologyAclBinding> setAclsForConsumers(Collection<String> principals, String topic) {
    principals.forEach(principal -> apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL));
    return new ArrayList<>();
  }

  @Override
  public List<TopologyAclBinding> setAclsForProducers(Collection<String> principals, String topic) {
    principals.forEach(principal -> apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL));
    return new ArrayList<>();
  }

  @Override
  public void setPredefinedRole(String principal, String predefinedRole, String topicPrefix) {
    apiClient.bind(principal, predefinedRole, topicPrefix, PREFIX);
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public List<TopologyAclBinding> setAclsForSchemaRegistry(String principal) {
    apiClient.bind(principal, SECURITY_ADMIN).forSchemaRegistry().apply();
    apiClient.bind(principal, RESOURCE_OWNER, "_schemas", LITERAL);
    apiClient.bind(principal, RESOURCE_OWNER, "schema-registry", "Group", LITERAL);
    return new ArrayList<>();
  }

  @Override
  public Map<String, List<TopologyAclBinding>> listAcls() {
    LOGGER.warn("Not implemented yet!");
    return new HashMap<>();
  }
}
