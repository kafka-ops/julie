package server.api.controllers;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import server.api.model.topology.Project;
import server.api.model.topology.Topic;
import server.api.model.topology.Topology;
import server.api.model.topology.users.Connector;
import server.api.model.topology.users.Consumer;
import server.api.model.topology.users.KStream;
import server.api.model.topology.users.Producer;
import server.api.services.TopologyService;

@Controller( value = "/topologies/{team}/projects/{projectName}/principals")
public class PrincipalsController {

  @Value("${micronaut.application.topology}")
  protected String topology;

  @Inject
  private TopologyService service;

  @Post(uri = "/consumers/{principalName}", processes = MediaType.APPLICATION_JSON)
  public HttpResponse createConsumer(
      @PathVariable String team,
      @PathVariable String projectName,
      @PathVariable String principalName) {

    Topology topology = service.findByTeam(team);

    Optional<Project> projectOptional = Optional.empty();
    for(Project p : topology.getProjects()) {
      if (p.getName().equalsIgnoreCase(projectName)) {
        projectOptional = Optional.of(p);
      }
    }

    Consumer consumer = new Consumer();
    consumer.setPrincipal("User:"+principalName);

    projectOptional.map(project -> {
      project.getConsumers().add(consumer);
      return project;
    });

    service.update(topology);

    return HttpResponse.ok().body(topology);
  }

  @Post(uri = "/producers/{principalName}", processes = MediaType.APPLICATION_JSON)
  public HttpResponse createProducer(
      @PathVariable String team,
      @PathVariable String projectName,
      @PathVariable String principalName) {

    Topology topology = service.findByTeam(team);

    Optional<Project> projectOptional = Optional.empty();
    for(Project p : topology.getProjects()) {
      if (p.getName().equalsIgnoreCase(projectName)) {
        projectOptional = Optional.of(p);
      }
    }

    Producer producer = new Producer();
    producer.setPrincipal("User:"+principalName);

    projectOptional.map(project -> {
      project.getProducers().add(producer);
      return project;
    });

    service.update(topology);

    return HttpResponse.ok().body(topology);
  }

  @Post(uri = "/streams/{principalName}", processes = MediaType.APPLICATION_JSON)
  public HttpResponse createStreams(
      @PathVariable String team,
      @PathVariable String projectName,
      @PathVariable String principalName,
      @NotNull @Body Map<String, List<String>> topics) {

    Topology topology = service.findByTeam(team);

    Optional<Project> projectOptional = Optional.empty();
    for(Project p : topology.getProjects()) {
      if (p.getName().equalsIgnoreCase(projectName)) {
        projectOptional = Optional.of(p);
      }
    }

    KStream stream = new KStream();
    stream.setPrincipal("User:"+principalName);
    stream.setTopics(topics);

    projectOptional.map(project -> {
      project.getStreams().add(stream);
      return project;
    });

    service.update(topology);

    return HttpResponse.ok().body(topology);
  }

  @Post(uri = "/connectors/{principalName}", processes = MediaType.APPLICATION_JSON)
  public HttpResponse createConnectors(
      @PathVariable String team,
      @PathVariable String projectName,
      @PathVariable String principalName,
      @NotNull @Body Map<String, Object> config) {

    Topology topology = service.findByTeam(team);

    Optional<Project> projectOptional = Optional.empty();
    for(Project p : topology.getProjects()) {
      if (p.getName().equalsIgnoreCase(projectName)) {
        projectOptional = Optional.of(p);
      }
    }

    Connector connector = new Connector();
    connector.setPrincipal("User:"+principalName);

    if ( config.containsKey("group") ) {
      connector.setGroup((String)config.get("group"));
    }

    if ( config.containsKey("status_topic") ) {
      connector.setStatus_topic((String)config.get("status_topic"));
    }

    if ( config.containsKey("offset_topic") ) {
      connector.setOffset_topic((String)config.get("offset_topic"));
    }

    if ( config.containsKey("configs_topic") ) {
      connector.setConfigs_topic((String)config.get("configs_topic"));
    }

    if (config.containsKey("topics")) {
      Map<String, List<String>> topics = (Map<String, List<String>>) config.get("topics");
      connector.setTopics(topics);
    }

    projectOptional.map(project -> {
      project.getConnectors().add(connector);
      return project;
    });

    service.update(topology);

    return HttpResponse.ok().body(topology);
  }

}
