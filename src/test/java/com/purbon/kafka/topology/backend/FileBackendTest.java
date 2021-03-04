package com.purbon.kafka.topology.backend;

import static com.purbon.kafka.topology.backend.FileBackend.ACLS_TAG;
import static com.purbon.kafka.topology.backend.FileBackend.SERVICE_ACCOUNTS_TAG;
import static com.purbon.kafka.topology.backend.FileBackend.STATE_FILE_NAME;
import static com.purbon.kafka.topology.backend.FileBackend.TOPICS_TAG;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.BackendController.Mode;
import com.purbon.kafka.topology.TestTopologyBuilder;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileBackendTest {

  private FileBackend backend;

  @Before
  public void setup() {
    backend = new FileBackend();
  }

  @After
  public void after() throws IOException {
    Files.deleteIfExists(Paths.get(STATE_FILE_NAME));
  }

  @Test
  public void testStoreAndLoadBindingsAndTopics() throws IOException {
    verifyStoreAndLoadWithPrincipal("principal");
  }

  @Test
  public void shouldHandlePrincipalWithSpace() throws IOException {
    verifyStoreAndLoadWithPrincipal("User:C=NO,CN=John Doe,emailAddress=john.doe@example.com");
  }

  @Test
  public void shouldHandlePrincipalWithUri() throws IOException {
    verifyStoreAndLoadWithPrincipal("SPIFFE:spiffe://example.com/foo/bar");
  }

  private void verifyStoreAndLoadWithPrincipal(final String principal) throws IOException {
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", principal, "LITERAL");

    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject().addTopic("foo").addTopic("bar");

    Topic fooTopic = builder.getTopic("foo");
    Topic barTopic = builder.getTopic("bar");

    backend.createOrOpen(Mode.TRUNCATE);
    backend.saveType(ACLS_TAG);
    backend.saveBindings(Collections.singleton(binding));
    backend.saveType(SERVICE_ACCOUNTS_TAG);
    backend.saveType(TOPICS_TAG);
    backend.saveTopics(new HashSet<>(Arrays.asList(fooTopic.toString(), barTopic.toString())));
    backend.close();

    backend = new FileBackend();
    backend.createOrOpen();

    Set<TopologyAclBinding> bindings = backend.loadBindings();
    Set<String> topics = backend.loadTopics();

    assertThat(bindings).hasSize(1);
    assertThat(bindings).contains(binding);
    assertThat(topics).hasSize(2);
    assertThat(topics).contains(fooTopic.toString());
    assertThat(topics).contains(barTopic.toString());
    backend.close();
  }
}
