package server.api.controllers;

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import javax.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import server.api.MongoContainer;
import server.api.model.topology.Project;
import server.api.model.topology.Topology;
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

  protected Topology createTopology(String team) {
    return service.create(team);
  }

  protected Topology  addProject(String team, String projectName) {
    Topology topology = service.findByTeam(team);

    Project project = new Project();
    project.setName(projectName);
    topology.addProject(project);

    service.update(topology);
    return topology;
  }
}
