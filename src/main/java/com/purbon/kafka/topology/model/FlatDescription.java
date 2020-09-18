package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.config.ConfigResource;

public class FlatDescription {
  Set<TopologyAclBinding> aclsBindings;
  Map<String, TopicDescription> topics;

  // TODO: add RBAC role-bindings

  @JsonCreator
  public FlatDescription(
      @JsonProperty("aclsBindings") Set<TopologyAclBinding> aclsBindings,
      @JsonProperty("topics") Map<String, TopicDescription> topics) {
    this.aclsBindings = aclsBindings;
    this.topics = topics;
  }

  public Set<TopologyAclBinding> getAclsBindings() {
    return aclsBindings;
  }

  public Map<String, TopicDescription> getTopics() {
    return topics;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FlatDescription)) return false;
    FlatDescription that = (FlatDescription) o;
    return Objects.equals(aclsBindings, that.aclsBindings) && Objects.equals(topics, that.topics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aclsBindings, topics);
  }

  public static FlatDescription extractFromCluster(AdminClient admin) {

    try {
      final ListTopicsResult listTopicsResult = admin.listTopics();
      final Set<String> topicNames = listTopicsResult.names().get();

      final Collection<ConfigResource> collect =
          topicNames.stream()
              .map(topicName -> new ConfigResource(ConfigResource.Type.TOPIC, topicName))
              .collect(Collectors.toSet());

      final DescribeConfigsResult describeConfigsResult = admin.describeConfigs(collect);
      final Map<ConfigResource, Config> configResourceConfigMap = describeConfigsResult.all().get();

      final Map<String, TopicDescription> tds = new HashMap<>();

      configResourceConfigMap.forEach(
          (configResource, config) -> {
            Map<String, String> topicConfig = new HashMap<>();
            config
                .entries()
                .forEach(
                    configEntry -> {
                      // TODO: discuss how to deal with default values
                      if (!configEntry.isDefault())
                        topicConfig.put(configEntry.name(), configEntry.value());
                    });
            final String topicName = configResource.name();
            tds.put(topicName, new TopicDescription(topicName, topicConfig));
          });

      // now get ACL entries
      Set<TopologyAclBinding> bindings = new HashSet<>();
      try {
        final DescribeAclsResult describeAclsResult = admin.describeAcls(AclBindingFilter.ANY);

        final Collection<AclBinding> aclBindingsFromCluster = describeAclsResult.values().get();

        aclBindingsFromCluster.forEach(b -> bindings.add(new TopologyAclBinding(b)));
      } catch (ExecutionException e) {
        // no ACLs present since security is diabled, just log the fact
        System.out.println("security disabled, no ACLs found");
      }
      return new FlatDescription(bindings, tds);

    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
    return null;
  }
}
