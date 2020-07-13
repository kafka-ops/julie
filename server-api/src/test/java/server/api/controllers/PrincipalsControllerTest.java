package server.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.http.HttpRequest;
import io.micronaut.test.annotation.MicronautTest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import server.api.model.topology.Topic;
import server.api.model.topology.Topology;
import server.api.model.topology.users.Consumer;
import server.api.model.topology.users.KStream;
import server.api.model.topology.users.Producer;

@MicronautTest
public class PrincipalsControllerTest extends BaseControllerTest {

  @Test
  void testCreateOKConsumerResponse() {

    createTopology("bar");
    addProject("bar", "p2");

    HttpRequest request = HttpRequest
        .POST("/topologies/bar/projects/p2/principals/consumers/bar", "");

    Topology topology = client
        .toBlocking()
        .retrieve(request, Topology.class);

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

    Topology topology = client
        .toBlocking()
        .retrieve(request, Topology.class);

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

    Topology topology = client
        .toBlocking()
        .retrieve(request, Topology.class);

    assertEquals("streams", topology.getTeam());
    assertEquals("p1", topology.getProjects().get(0).getName());

    KStream pro = topology.getProjects().get(0).getStreams().get(0);
    assertEquals("User:bar", pro.getPrincipal());

    List<String> readTopics = pro.getTopics().get("read");
    assertEquals("topicA", readTopics.get(0));

    List<String> writeTopics = pro.getTopics().get("write");
    assertEquals("topicB", writeTopics.get(0));

  }

}
