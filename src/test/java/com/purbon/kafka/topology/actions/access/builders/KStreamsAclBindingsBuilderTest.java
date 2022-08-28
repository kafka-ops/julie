package com.purbon.kafka.topology.actions.access.builders;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.roles.acls.AclsBindingsBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KStreamsAclBindingsBuilderTest {

  private KStreamsAclBindingsBuilder builder;
  private AclsBindingsBuilder aclsBindingsBuilder;

  @BeforeEach
  public void setUp() {
    aclsBindingsBuilder = new AclsBindingsBuilder(new Configuration());
  }

  @Test
  void streamsWithoutApplicationId() {
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, singletonList("topicA"));
    topics.put(KStream.WRITE_TOPICS, singletonList("topicB"));

    KStream app = new KStream("User:user", topics);
    String topicPrefix = "topicPrefix";
    builder = new KStreamsAclBindingsBuilder(aclsBindingsBuilder, app, topicPrefix);
    assertThat(builder.getAclBindings().getAclBindings())
        .anyMatch(
            b ->
                b.getResourceType().equals(ResourceType.TOPIC.name())
                    && b.getResourceName().equals(topicPrefix));
    assertThat(builder.getAclBindings().getAclBindings())
        .anyMatch(
            b ->
                b.getResourceType().equals(ResourceType.GROUP.name())
                    && b.getResourceName().equals(topicPrefix));
  }

  @Test
  void streamWithApplicationId() {
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, singletonList("topicA"));
    topics.put(KStream.WRITE_TOPICS, singletonList("topicB"));

    String applicationId = "applicationId";
    KStream app = new KStream("User:user", topics, Optional.of(applicationId));
    builder = new KStreamsAclBindingsBuilder(aclsBindingsBuilder, app, "topicPrefix");
    assertThat(builder.getAclBindings().getAclBindings())
        .anyMatch(
            b ->
                b.getResourceType().equals(ResourceType.TOPIC.name())
                    && b.getResourceName().equals(applicationId));
    assertThat(builder.getAclBindings().getAclBindings())
        .anyMatch(
            b ->
                b.getResourceType().equals(ResourceType.GROUP.name())
                    && b.getResourceName().equals(applicationId));
  }
}
