package com.purbon.kafka.topology.roles;

import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_WRITE;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.RESOURCE_OWNER;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RBACProvider implements AccessControlProvider {

  public static final String LITERAL = "LITERAL";
  public static final String PREFIX = "PREFIX";
  private final MDSApiClient apiClient;

  public RBACProvider(MDSApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public void clearAcls() {

  }

  @Override
  public void setAclsForConnect(String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {

    apiClient.bind(principal, DEVELOPER_READ, topicPrefix, PREFIX);
    readTopics.forEach(topic -> apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL));
    writeTopics.forEach(topic -> apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL));

    String[] resources = new String[]{"Topic:connect-configs",
        "Topic:connect-offsets",
        "Topic:connect-status",
        "Group:connect-cluster",
        "Group:secret-registry",
        "Topic:_confluent-secrets"};

    Arrays
        .asList(resources)
        .forEach(resourceObject -> {
          String[] elements = resourceObject.split(":");
          String resource = elements[0];
          String resourceType = elements[1];
          apiClient.bind(principal, RESOURCE_OWNER, resource, resourceType, LITERAL);
        });
  }

  @Override
  public void setAclsForStreamsApp(String principal, String topicPrefix, List<String> readTopics,
      List<String> writeTopics) {

    apiClient.bind(principal, DEVELOPER_READ, topicPrefix, PREFIX);
    readTopics.forEach(topic -> apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL));
    writeTopics.forEach(topic -> apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL));

    apiClient.bind(principal, RESOURCE_OWNER, topicPrefix, PREFIX);
    apiClient.bind(principal, RESOURCE_OWNER, topicPrefix, "Group", PREFIX);

  }

  @Override
  public void setAclsForConsumers(Collection<String> principals, String topic) {
    principals.forEach(principal -> apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL));
  }

  @Override
  public void setAclsForProducers(Collection<String> principals, String topic) {
    principals.forEach(principal -> apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL));
  }

  @Override
  public void setPredefinedRole(String principal, String predefinedRole, String topicPrefix) {
    apiClient.bind(principal, predefinedRole, topicPrefix, PREFIX);
  }
}
