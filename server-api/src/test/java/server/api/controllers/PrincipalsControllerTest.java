package server.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.http.HttpRequest;
import io.micronaut.test.annotation.MicronautTest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import server.api.model.topology.Topic;
import server.api.model.topology.Topology;
import server.api.model.topology.users.Consumer;
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

}
