package com.purbon.kafka.topology.clients;

import com.purbon.kafka.topology.api.mds.Response;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ExtHttpClient {

  private static final Logger LOGGER = LogManager.getLogger(ExtHttpClient.class);

  private final long DEFAULT_TIMEOUT_MS = 60000;

  private final HttpClient httpClient = HttpClient.newBuilder().build();
  private final String server;

  public ExtHttpClient(String server) {
    this.server = server;
  }

  protected HttpRequest buildGetRequest(String url, String token) {
    return buildGetRequest(url, token, DEFAULT_TIMEOUT_MS);
  }

  protected HttpRequest buildGetRequest(String url, String token, long timeoutMs) {
    return HttpRequest.newBuilder()
        .uri(URI.create(server + url))
        .timeout(Duration.ofMillis(timeoutMs))
        .header("accept", " application/json")
        .header("Authorization", "Basic " + token)
        .GET()
        .build();
  }

  protected Response doGet(HttpRequest request) throws IOException {
    LOGGER.debug("GET.request: " + request);
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      LOGGER.debug("GET.response: " + response);
      return new Response(response);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  protected String doPost(HttpRequest request) throws IOException {
    LOGGER.debug("POST.request: " + request);
    String result = "";
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      LOGGER.debug("POST.response: " + response);
      int statusCode = response.statusCode();
      if (statusCode < 200 || statusCode > 299) {
        throw new IOException(
            "Something happened with the connection, response status code: "
                + statusCode
                + " "
                + request);
      }

      if (response.body() != null) {
        result = response.body();
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    return result;
  }

  protected void doDelete(HttpRequest request) throws IOException {
    LOGGER.debug("DELETE.request: " + request);
    String result = "";
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.body() != null) {
        result = response.body();
      }
      LOGGER.debug("DELETE.response: " + response + " result: " + result);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
}
