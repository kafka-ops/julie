package com.purbon.kafka.topology.roles;

import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_WRITE;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import java.util.Collection;
import java.util.List;

public class RBACProvider implements AccessControlProvider {

  public static final String LITERAL = "LITERAL";
  private final MDSApiClient apiClient;

  public RBACProvider(MDSApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public void clearAcls() {

  }

  @Override
  public void setAclsForConnect(String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {

  }

  @Override
  public void setAclsForStreamsApp(String principal, String topicPrefix, List<String> readTopics,
      List<String> writeTopics) {

  }

  @Override
  public void setAclsForConsumers(Collection<String> principals, String topic) {
    principals.forEach(principal -> apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL));
  }

  @Override
  public void setAclsForProducers(Collection<String> principals, String topic) {
    principals.forEach(principal -> apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL));

  }
}
