package server.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.purbon.kafka.topology.model.Topic;
import io.micronaut.http.HttpRequest;
import io.micronaut.test.annotation.MicronautTest;
import java.util.Collections;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import java.util.Map;
import server.api.models.TopologyDeco;

@MicronautTest
public class TopicControllerTest extends BaseControllerTest {

  @Test
  void testCreateOKResponse() {

    createTopology("foo");
    addProject("foo", "p1");

    HttpRequest request = HttpRequest
        .POST("/topologies/foo/projects/p1/topics/t1", Collections.EMPTY_MAP);

    TopologyDeco topology = client
        .toBlocking()
        .retrieve(request, TopologyDeco.class);

    assertEquals("foo", topology.getTeam());
    assertEquals("p1", topology.getProjects().get(0).getName());
    assertEquals("t1", topology.getProjects().get(0).getTopics().get(0).getName());

  }

  @Test
  void testCreateOKwithConfigurationResponse() {

    createTopology("foo");
    addProject("foo", "p1");

    Map<String, Object> config = new HashMap<>();
    config.put("replication_factor", 1);
    config.put("num_partitions", 2);

    HttpRequest request = HttpRequest
        .POST("/topologies/foo/projects/p1/topics/t1", config);

    TopologyDeco topology = client
        .toBlocking()
        .retrieve(request, TopologyDeco.class);

    assertEquals("foo", topology.getTeam());
    assertEquals("p1", topology.getProjects().get(0).getName());

    Topic topic = topology.getProjects().get(0).getTopics().get(0);
    assertEquals("t1", topic.getName());
    assertEquals("1", topic.getConfig().get("replication_factor"));
    assertEquals("2", topic.getConfig().get("num_partitions"));


  }

}
