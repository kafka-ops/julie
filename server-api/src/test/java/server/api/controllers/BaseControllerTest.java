package server.api.controllers;

import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Project;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import javax.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import server.api.MongoContainer;
import server.api.models.TopologyDeco;
import server.api.services.TopologyService;

public class BaseControllerTest {


  public static MongoContainer mongo = new MongoContainer();

  @BeforeAll
  public static void setup() {
    mongo.start();
  }

  @AfterAll
  public static void down() {
    mongo.stop();
  }

  @Inject
  EmbeddedServer server;

  @Inject
  @Client("/")
  HttpClient client;

  @Inject
  TopologyService service;

  protected TopologyDeco createTopology(String team) {
    return service.create(team);
  }

  protected TopologyDeco addProject(String team, String projectName) {
    TopologyDeco topology = service.findByTeam(team);

    Project project = new ProjectImpl();
    project.setName(projectName);
    topology.addProject(project);

    service.update(topology);
    return topology;
  }
}
