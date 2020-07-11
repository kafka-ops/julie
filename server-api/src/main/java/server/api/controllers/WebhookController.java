package server.api.controllers;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

@Controller("/webhook")
public class WebhookController {

  @Post()
  public HttpResponse receiveNotification() {
    return HttpResponse.ok();
  }
}
