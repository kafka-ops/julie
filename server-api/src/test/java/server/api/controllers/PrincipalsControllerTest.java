package server.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import io.micronaut.http.HttpRequest;
import io.micronaut.test.annotation.MicronautTest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import server.api.models.TopologyDeco;

@MicronautTest
public class PrincipalsControllerTest extends BaseControllerTest {

  @Test
  void testCreateOKConsumerResponse() {

    createTopology("bar");
    addProject("bar", "p2");

    HttpRequest request = HttpRequest
        .POST("/topologies/bar/projects/p2/principals/consumers/bar", "");

    TopologyDeco topology = client
        .toBlocking()
        .retrieve(request, TopologyDeco.class);

    assertEquals("bar", topology.getTeam());
    assertEquals("p2", topology.getProjects().get(0).getName());

    Consumer con = topology.getProjects().get(0).getConsumers().get(0);
    assertEquals("User:bar", con.getPrincipal());
  }

  @Test
  void testCreateOKProducerResponse() {

    createTopology("foo");
    addProject("foo", "p1");

    HttpRequest request = HttpRequest
        .POST("/topologies/foo/projects/p1/principals/producers/bar", "");

    TopologyDeco topology = client
        .toBlocking()
        .retrieve(request, TopologyDeco.class);

    assertEquals("foo", topology.getTeam());
    assertEquals("p1", topology.getProjects().get(0).getName());

    Producer pro = topology.getProjects().get(0).getProducers().get(0);
    assertEquals("User:bar", pro.getPrincipal());
  }

  @Test
  void testCreateOKStreamsResponse() {

    createTopology("streams");
    addProject("streams", "p1");

    Map<String, List<String>> topics = new HashMap<>();
    topics.put("read", Collections.singletonList("topicA"));
    topics.put("write", Collections.singletonList("topicB"));

    HttpRequest request = HttpRequest
        .POST("/topologies/streams/projects/p1/principals/streams/bar", topics);

    TopologyDeco topology = client
        .toBlocking()
        .retrieve(request, TopologyDeco.class);

    assertEquals("streams", topology.getTeam());
    assertEquals("p1", topology.getProjects().get(0).getName());

    KStream pro = topology.getProjects().get(0).getStreams().get(0);
    assertEquals("User:bar", pro.getPrincipal());

    List<String> readTopics = pro.getTopics().get("read");
    assertEquals("topicA", readTopics.get(0));

    List<String> writeTopics = pro.getTopics().get("write");
    assertEquals("topicB", writeTopics.get(0));

  }

  @Test
  void testCreateOKConnectorResponse() {

    createTopology("connector");
    addProject("connector", "p1");

    Map<String, Object> config = new HashMap<>();

    HttpRequest request = HttpRequest
        .POST("/topologies/connector/projects/p1/principals/connectors/bar", config);

    TopologyDeco topology = client
        .toBlocking()
        .retrieve(request, TopologyDeco.class);

    assertEquals("connector", topology.getTeam());
    assertEquals("p1", topology.getProjects().get(0).getName());

    Connector con = topology.getProjects().get(0).getConnectors().get(0);

    assertEquals("User:bar", con.getPrincipal());
    assertEquals("connect-configs", con.configsTopicString());
    assertEquals("connect-cluster", con.groupString());
  }

  @Test
  void testCreateOKConnectorWithConfigResponse() {

    createTopology("connconfig");
    addProject("connconfig", "p1");

    Map<String, Object> config = new HashMap<>();
    config.put("group", "foo");
    config.put("configs_topic", "configs");

    Map<String, List<String>> topics = new HashMap<>();
    topics.put("read", Collections.singletonList("topicA"));
    topics.put("write", Collections.singletonList("topicB"));

    config.put("topics", topics);

    HttpRequest request = HttpRequest
        .POST("/topologies/connconfig/projects/p1/principals/connectors/bar", config);

    TopologyDeco topology = client
        .toBlocking()
        .retrieve(request, TopologyDeco.class);

    assertEquals("connconfig", topology.getTeam());
    assertEquals("p1", topology.getProjects().get(0).getName());

    Connector con = topology.getProjects().get(0).getConnectors().get(0);

    assertEquals("User:bar", con.getPrincipal());
    assertEquals("configs", con.configsTopicString());
    assertEquals("foo", con.groupString());

    List<String> readTopics = con.getTopics().get("read");
    assertEquals("topicA", readTopics.get(0));

    List<String> writeTopics = con.getTopics().get("write");
    assertEquals("topicB", writeTopics.get(0));

  }

}
