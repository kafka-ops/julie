package server.api.controllers;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import java.util.Optional;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import server.api.model.topology.Project;
import server.api.model.topology.Topic;
import server.api.model.topology.Topology;
import server.api.services.TopologyService;
import java.util.Map;

@Controller( value = "/topologies/{team}/projects/{projectName}/topics")
public class TopicController {

  @Value("${micronaut.application.topology}")
  protected String topology;

  @Inject
  private TopologyService service;


  @Post(uri = "/{topicName}", processes = MediaType.APPLICATION_JSON)
  public HttpResponse create(
      @PathVariable String team,
      @PathVariable String projectName,
      @PathVariable String topicName,
      @NotNull @Body Map<String, String> config) {

    Topology topology = service.findByTeam(team);

    Topic topic = new Topic();
    topic.setName(topicName);
    if (!config.isEmpty())
      topic.setConfig(config);
    Optional<Project> project = Optional.empty();
    for(Project p : topology.getProjects()) {
      if (p.getName().equalsIgnoreCase(projectName)) {
        project = Optional.of(p);
      }
    }

    project.map(project1 -> {
      project1.addTopic(topic);
      return project1;
    });

    service.update(topology);

    return HttpResponse.ok().body(topology);
  }
}
