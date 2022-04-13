package com.purbon.kafka.topology.actions.topics;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Topic;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class DeleteTopicsActionTest {

  @Mock TopologyBuilderAdminClient adminClient;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Test
  public void shouldComposeDetailedViewOfProperties() {
    Topic topic = new Topic("foo");
    topic.setProjectPrefix("bar");
    topic.setConfig(Collections.singletonMap("foo", "bar"));
    var action = new DeleteTopics(adminClient, Collections.singletonList(topic.toString()));

    var refs = action.refs();
    assertThat(refs).hasSize(1);
    var ref = refs.get(0);
    assertThat(ref)
        .contains(
            "\"resource_name\" : \"rn://delete.topic/com.purbon.kafka.topology.actions.topics.DeleteTopics$1/bar.foo\"");
    assertThat(ref).contains("\"topic\" : \"bar.foo\",");
  }
}
