package kafka.ops.topology.backend;

import static kafka.ops.topology.backend.FileBackend.ACLS_TAG;
import static kafka.ops.topology.backend.FileBackend.SERVICE_ACCOUNTS_TAG;
import static kafka.ops.topology.backend.FileBackend.STATE_FILE_NAME;
import static kafka.ops.topology.backend.FileBackend.TOPICS_TAG;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import kafka.ops.topology.model.Impl.ProjectImpl;
import kafka.ops.topology.model.Impl.TopicImpl;
import kafka.ops.topology.model.Impl.TopologyImpl;
import kafka.ops.topology.model.Project;
import kafka.ops.topology.model.Topic;
import kafka.ops.topology.model.Topology;
import kafka.ops.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import kafka.ops.topology.BackendController;
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
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("project");
    topology.setProjects(singletonList(project));

    Topic topic = new TopicImpl("foo");
    Topic topicBar = new TopicImpl("bar");
    project.setTopics(Arrays.asList(topic, topicBar));

    backend.createOrOpen(BackendController.Mode.TRUNCATE);
    backend.saveType(ACLS_TAG);
    backend.saveBindings(Collections.singleton(binding));
    backend.saveType(SERVICE_ACCOUNTS_TAG);
    backend.saveType(TOPICS_TAG);
    backend.saveTopics(new HashSet<>(Arrays.asList(topic.toString(), topicBar.toString())));
    backend.close();

    backend = new FileBackend();
    backend.createOrOpen();

    Set<TopologyAclBinding> bindings = backend.loadBindings();
    Set<String> topics = backend.loadTopics();

    assertThat(bindings).hasSize(1);
    assertThat(bindings).contains(binding);
    assertThat(topics).hasSize(2);
    assertThat(topics).contains(topic.toString());
    assertThat(topics).contains(topicBar.toString());
  }
}
