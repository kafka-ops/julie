package com.purbon.kafka.topology.actions.access.builders;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.roles.acls.AclsBindingsBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Before;
import org.junit.Test;

public class BuildBindingsForKStreamsTest {

  private BuildBindingsForKStreams action;
  private AclsBindingsBuilder aclsBindingsBuilder;

  @Before
  public void setUp() {
    aclsBindingsBuilder = new AclsBindingsBuilder(new Configuration());
  }

  @Test
  public void testStreamsWithoutApplicationId() throws IOException {
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, singletonList("topicA"));
    topics.put(KStream.WRITE_TOPICS, singletonList("topicB"));

    KStream app = new KStream("User:user", topics);
    String topicPrefix = "topicPrefix";
    action = new BuildBindingsForKStreams(aclsBindingsBuilder, app, topicPrefix);
    action.execute();
    assertThat(action.getAclBindings())
        .anyMatch(
            b ->
                b.getResourceType() == ResourceType.TOPIC.name()
                    && b.getResourceName().equals(topicPrefix));
    assertThat(action.getAclBindings())
        .anyMatch(
            b ->
                b.getResourceType() == ResourceType.GROUP.name()
                    && b.getResourceName().equals(topicPrefix));
  }

  @Test
  public void testStreamWithApplicationId() throws IOException {
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, singletonList("topicA"));
    topics.put(KStream.WRITE_TOPICS, singletonList("topicB"));

    String applicationId = "applicationId";
    KStream app = new KStream("User:user", topics, Optional.of(applicationId));
    action = new BuildBindingsForKStreams(aclsBindingsBuilder, app, "topicPrefix");
    action.execute();
    assertThat(action.getAclBindings())
        .anyMatch(
            b ->
                b.getResourceType() == ResourceType.TOPIC.name()
                    && b.getResourceName().equals(applicationId));
    assertThat(action.getAclBindings())
        .anyMatch(
            b ->
                b.getResourceType() == ResourceType.GROUP.name()
                    && b.getResourceName().equals(applicationId));
  }
}
