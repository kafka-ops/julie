package server.api.controllers;

import server.api.model.topology.Project;
import server.api.model.topology.Topology;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import server.api.services.TopologyService;
import java.util.Map;

@Controller("/topologies")
public class TopologyController {

  @Value("${micronaut.application.topology}")
  protected String topology;

  @Inject
  private TopologyService service;

  @Get(processes = MediaType.APPLICATION_JSON)
  public HttpResponse index() {
    List<Topology> all = service.all();
    return HttpResponse.ok().body(all);
  }

  @Get(uri = "/{team}", processes = MediaType.APPLICATION_JSON)
  public HttpResponse<Topology> get(@PathVariable String team) {
    Topology topology = service.findByTeam(team);
    return HttpResponse.ok().body(topology);
  }

  @Post(uri = "/{team}", processes = MediaType.APPLICATION_JSON)
  public HttpResponse create(@PathVariable String team) {
    Topology topology = service.create(team);

    Map<String, Object> response = new HashMap<>();
    response.put("topology", topology.getTeam());
    response.put("created", System.currentTimeMillis());
    return HttpResponse.ok().body(response);
  }

}
