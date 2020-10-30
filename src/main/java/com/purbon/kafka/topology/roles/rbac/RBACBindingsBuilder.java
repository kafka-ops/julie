package com.purbon.kafka.topology.roles.rbac;

import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.DEVELOPER_WRITE;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.RESOURCE_OWNER;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.SECURITY_ADMIN;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.SYSTEM_ADMIN;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.model.Component;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RBACBindingsBuilder implements BindingsBuilderProvider {

  public static final String LITERAL = "LITERAL";
  public static final String PREFIX = "PREFIXED";

  private final MDSApiClient apiClient;

  public RBACBindingsBuilder(MDSApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForConnect(Connector connector, String topicPrefix) {

    String principal = connector.getPrincipal();
    List<String> readTopics = connector.getTopics().get("read");
    List<String> writeTopics = connector.getTopics().get("write");

    List<TopologyAclBinding> bindings = new ArrayList<>();

    TopologyAclBinding secAdminBinding =
        apiClient.bind(principal, SECURITY_ADMIN).forKafkaConnect(connector).apply();
    bindings.add(secAdminBinding);

    TopologyAclBinding readBinding = apiClient.bind(principal, DEVELOPER_READ, topicPrefix, PREFIX);
    bindings.add(readBinding);

    if (readTopics != null && !readTopics.isEmpty()) {
      readTopics.forEach(
          topic -> {
            TopologyAclBinding binding = apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL);
            bindings.add(binding);
          });
    }
    if (writeTopics != null && !writeTopics.isEmpty()) {
      writeTopics.forEach(
          topic -> {
            TopologyAclBinding binding = apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL);
            bindings.add(binding);
          });
    }

    String[] resources =
        new String[] {
          "Topic:" + connector.configsTopicString(),
          "Topic:" + connector.offsetTopicString(),
          "Topic:" + connector.statusTopicString(),
          "Group:" + connector.groupString(),
          "Group:secret-registry",
          "Topic:_confluent-secrets"
        };

    Arrays.asList(resources)
        .forEach(
            resourceObject -> {
              String[] elements = resourceObject.split(":");
              String resource = elements[1];
              String resourceType = elements[0];
              TopologyAclBinding binding =
                  apiClient.bind(principal, RESOURCE_OWNER, resource, resourceType, LITERAL);
              bindings.add(binding);
            });
    return bindings;
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    List<TopologyAclBinding> bindings = new ArrayList<>();

    TopologyAclBinding binding = apiClient.bind(principal, DEVELOPER_READ, topicPrefix, PREFIX);
    bindings.add(binding);

    readTopics.forEach(
        topic -> {
          TopologyAclBinding readBinding =
              apiClient.bind(principal, DEVELOPER_READ, topic, LITERAL);
          bindings.add(readBinding);
        });
    writeTopics.forEach(
        topic -> {
          TopologyAclBinding writeBinding =
              apiClient.bind(principal, DEVELOPER_WRITE, topic, LITERAL);
          bindings.add(writeBinding);
        });

    binding = apiClient.bind(principal, RESOURCE_OWNER, topicPrefix, PREFIX);
    bindings.add(binding);
    binding = apiClient.bind(principal, RESOURCE_OWNER, topicPrefix, "Group", PREFIX);
    bindings.add(binding);

    return bindings;
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForConsumers(
      Collection<Consumer> consumers, String resource, boolean prefixed) {
    String patternType = prefixed ? PREFIX : LITERAL;
    List<TopologyAclBinding> bindings = new ArrayList<>();
    consumers.forEach(
        consumer -> {
          TopologyAclBinding binding =
              apiClient.bind(consumer.getPrincipal(), DEVELOPER_READ, resource, patternType);
          bindings.add(binding);
          binding =
              apiClient.bind(
                  consumer.getPrincipal(),
                  RESOURCE_OWNER,
                  consumer.groupString(),
                  "Group",
                  LITERAL);
          bindings.add(binding);
        });
    return bindings;
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForProducers(
      Collection<Producer> producers, String resource, boolean prefixed) {
    String patternType = prefixed ? PREFIX : LITERAL;
    List<TopologyAclBinding> bindings = new ArrayList<>();
    producers.forEach(
        producer -> {
          TopologyAclBinding binding =
              apiClient.bind(producer.getPrincipal(), DEVELOPER_WRITE, resource, patternType);
          bindings.add(binding);
        });
    return bindings;
  }

  @Override
  public TopologyAclBinding setPredefinedRole(
      String principal, String predefinedRole, String topicPrefix) {
    return apiClient.bind(principal, predefinedRole, topicPrefix, PREFIX);
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForSchemaRegistry(
      SchemaRegistryInstance schemaRegistry) {
    String principal = schemaRegistry.getPrincipal();
    List<TopologyAclBinding> bindings = new ArrayList<>();
    TopologyAclBinding binding =
        apiClient.bind(principal, RESOURCE_OWNER, schemaRegistry.topicString(), LITERAL);
    bindings.add(binding);
    binding =
        apiClient.bind(principal, RESOURCE_OWNER, schemaRegistry.groupString(), "Group", LITERAL);
    bindings.add(binding);
    binding = apiClient.bind(principal, SECURITY_ADMIN).forSchemaRegistry().apply();
    bindings.add(binding);

    return bindings;
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForControlCenter(String principal, String appId) {
    TopologyAclBinding binding = apiClient.bind(principal, SYSTEM_ADMIN).forControlCenter().apply();
    return Collections.singletonList(binding);
  }

  @Override
  public List<TopologyAclBinding> setClusterLevelRole(
      String role, String principal, Component component) throws IOException {

    ClusterLevelRoleBuilder clusterLevelRoleBuilder = apiClient.bind(principal, role);
    TopologyAclBinding binding;
    switch (component) {
      case KAFKA:
        binding = clusterLevelRoleBuilder.forKafka().apply();
        break;
      case SCHEMA_REGISTRY:
        binding = clusterLevelRoleBuilder.forSchemaRegistry().apply();
        break;
      case KAFKA_CONNECT:
        binding = clusterLevelRoleBuilder.forKafkaConnect().apply();
        break;
      default:
        throw new IOException("Non valid component selected");
    }
    return Collections.singletonList(binding);
  }

  @Override
  public List<TopologyAclBinding> setSchemaAuthorization(String principal, List<String> subjects) {
    return subjects.stream()
        .map(subject -> apiClient.bind(principal, RESOURCE_OWNER).forSchemaSubject(subject).apply())
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> setConnectorAuthorization(
      String principal, List<String> connectors) {
    return connectors.stream()
        .map(
            connector ->
                apiClient.bind(principal, RESOURCE_OWNER).forAKafkaConnector(connector).apply())
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
