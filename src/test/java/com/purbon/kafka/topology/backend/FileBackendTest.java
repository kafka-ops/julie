package com.purbon.kafka.topology.backend;

import static com.purbon.kafka.topology.BackendController.STATE_FILE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.BackendController.Mode;
import com.purbon.kafka.topology.TestTopologyBuilder;
import com.purbon.kafka.topology.api.mds.ClusterIDs;
import com.purbon.kafka.topology.api.mds.RequestScope;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileBackendTest {

  private FileBackend backend;

  @BeforeEach
  public void setup() {
    backend = new FileBackend();
  }

  @AfterEach
  public void after() throws IOException {
    Files.deleteIfExists(Paths.get(STATE_FILE_NAME));
  }

  @Test
  void storeAndLoadBindingsAndTopics() throws IOException {
    verifyStoreAndLoadWithPrincipal("principal");
  }

  @Test
  void shouldHandlePrincipalWithSpace() throws IOException {
    verifyStoreAndLoadWithPrincipal("User:C=NO,CN=John Doe,emailAddress=john.doe@example.com");
  }

  @Test
  void shouldHandlePrincipalWithUri() throws IOException {
    verifyStoreAndLoadWithPrincipal("SPIFFE:spiffe://example.com/foo/bar");
  }

  @Test
  void bindingSerdes() throws JsonProcessingException {
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(),
            "foo",
            "*",
            AclOperation.CREATE.name(),
            "User:foo",
            PatternType.LITERAL.name());

    RequestScope scope = new RequestScope();
    ClusterIDs clusterIDs = new ClusterIDs();
    clusterIDs.setKafkaClusterId("kafka");
    scope.setClusters(clusterIDs.forKafka().asMap());
    binding.setScope(scope);

    scope.addResource("type", "name", "pattern");

    String content = JSON.asString(binding);
    TopologyAclBinding newBindings =
        (TopologyAclBinding) JSON.toObject(content, TopologyAclBinding.class);

    assertThat(newBindings.getScope().getResources()).hasSize(1);
    assertThat(newBindings.getScope().clusterIDs()).hasSize(1);
  }

  @Test
  void shouldParseStateFileSuccessfully() throws IOException {
    File file = TestUtils.getResourceFile("/stateFile.json");
    String content = Files.readString(file.toPath());

    var backend = (BackendState) JSON.toObject(content, BackendState.class);
    assertThat(backend.getTopics()).hasSize(6);
    assertThat(backend.getBindings()).hasSize(49);
    assertThat(backend.getAccounts()).hasSize(0);
  }

  @Test
  void shouldParseOldStyleStateFileSuccessfully() throws IOException {
    File file = TestUtils.getResourceFile("/old-style-state-file.txt");
    final BackendState state = new FileBackend().load(file.toPath());
    assertThat(state.getTopics()).hasSize(2);
    assertThat(state.getBindings()).hasSize(3);
    assertThat(state.getAccounts()).hasSize(0);
  }

  private void verifyStoreAndLoadWithPrincipal(final String principal) throws IOException {
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", principal, "LITERAL");

    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject().addTopic("foo").addTopic("bar");

    Topic fooTopic = builder.getTopic("foo");
    Topic barTopic = builder.getTopic("bar");

    BackendState state = new BackendState();
    state.addBindings(Collections.singleton(binding));
    state.addTopics(Arrays.asList(fooTopic.toString(), barTopic.toString()));

    backend.createOrOpen(Mode.TRUNCATE);
    backend.save(state);
    backend.close();

    backend = new FileBackend();
    backend.createOrOpen();

    BackendState recoveredState = backend.load();

    Set<TopologyAclBinding> bindings = recoveredState.getBindings();
    Set<String> topics = recoveredState.getTopics();

    assertThat(bindings).hasSize(1);
    assertThat(bindings).contains(binding);
    assertThat(topics).hasSize(2);
    assertThat(topics).contains(fooTopic.toString());
    assertThat(topics).contains(barTopic.toString());
    backend.close();
  }
}
