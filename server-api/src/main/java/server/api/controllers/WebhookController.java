package server.api.controllers;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import java.io.IOException;
import java.nio.file.Paths;
import javax.inject.Inject;
import server.api.models.GithubHook;
import server.api.services.GitManager;
import server.api.services.KafkaTopologyBuilderService;

@Controller("/webhook")
public class WebhookController {

  @Value("${micronaut.application.topology}")
  protected String topology;

  @Inject
  private GitManager engine;

  @Inject
  private KafkaTopologyBuilderService service;

  @Post(processes = MediaType.APPLICATION_JSON)
  public HttpResponse receive(HttpRequest<GithubHook> request) throws IOException {

    GithubHook body = request.getBody().get();

    try {
      String rootPath = engine.cloneOrPull(
          body.getRepository().getClone_url(),
          body.getRepository().getName()
      );

      service.sync(Paths
          .get(rootPath, topology)
          .toString());

      return HttpResponse
          .ok(rootPath);
    } catch (Exception ex) {
      return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
  }
}
