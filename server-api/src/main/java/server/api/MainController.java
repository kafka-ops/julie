package server.api;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import server.api.model.KafkaTopologyBuilderApi;

@Controller("/")
public class MainController {

  @Get( produces = MediaType.APPLICATION_JSON)
  public KafkaTopologyBuilderApi index() {
    KafkaTopologyBuilderApi api = new KafkaTopologyBuilderApi();
    return api;
  }
}
