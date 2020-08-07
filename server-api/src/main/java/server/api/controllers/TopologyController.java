package server.api.controllers;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import server.api.models.TopologyDeco;
import server.api.services.KafkaTopologyBuilderService;
import server.api.services.TopologyService;

@Controller("/topologies")
public class TopologyController {

  @Value("${micronaut.application.topology}")
  protected String topology;

  @Inject
  private KafkaTopologyBuilderService builderService;

  @Inject
  private TopologyService service;

  @Get(processes = MediaType.APPLICATION_JSON)
  public HttpResponse indexTopology() {
    List<TopologyDeco> all = service.all();
    return HttpResponse.ok().body(all);
  }

  @Get(uri = "/{team}", processes = MediaType.APPLICATION_JSON)
  public HttpResponse<TopologyDeco> get(@PathVariable String team) {
    TopologyDeco topology = service.findByTeam(team);
    return HttpResponse.ok().body(topology);
  }

  @Post(uri = "/{team}", processes = MediaType.APPLICATION_JSON)
  public HttpResponse createTopology(@PathVariable String team) {
    TopologyDeco topology = service.create(team);

    Map<String, Object> response = new HashMap<>();
    response.put("topology", topology.getTeam());
    response.put("created", System.currentTimeMillis());
    return HttpResponse.ok().body(response);
  }

  @Post(uri = "/{team}/apply", processes = MediaType.APPLICATION_JSON)
  public HttpResponse apply(@PathVariable String team) {
    TopologyDeco topology = service.findByTeam(team);

    try {
      builderService.sync(topology);
      return HttpResponse.ok();
    } catch (Exception ex) {
      return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }

}
