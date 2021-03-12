package com.purbon.kafka.topology.clients;

import com.purbon.kafka.topology.api.mds.Response;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class JulieHttpClient {

  private static final Logger LOGGER = LogManager.getLogger(JulieHttpClient.class);

  private final long DEFAULT_TIMEOUT_MS = 60000;

  private final HttpClient httpClient = HttpClient.newBuilder().build();
  private final String server;
  private String token;

  public JulieHttpClient(String server) {
    this.server = server;
  }

  private HttpRequest.Builder setupARequest(String url, String token, long timeoutMs) {
    return HttpRequest.newBuilder()
        .uri(URI.create(server + url))
        .timeout(Duration.ofMillis(timeoutMs))
        .header("accept", " application/json")
        .header("Content-Type", "application/json")
        .header("Authorization", "Basic " + token);
  }

  public void login(String user, String password) {
    String userAndPassword = user + ":" + password;
    this.token = Base64.getEncoder().encodeToString(userAndPassword.getBytes());
  }

  protected Response doGet(String url) throws IOException {
    HttpRequest request = getRequest(url, token, DEFAULT_TIMEOUT_MS);
    return doGet(request);
  }

  private HttpRequest getRequest(String url, String token, long timeoutMs) {
    return setupARequest(url, token, timeoutMs).GET().build();
  }

  private Response doGet(HttpRequest request) throws IOException {
    LOGGER.debug("method: " + request.method() + " request.uri: " + request.uri());
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      LOGGER.debug("method: " + request.method() + " response: " + response);
      return new Response(response);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  protected String doPost(String url, String body) throws IOException {
    LOGGER.debug("doPost: " + url + " body: " + body);
    HttpRequest request = postRequest(url, body, token, DEFAULT_TIMEOUT_MS);
    return doRequest(request);
  }

  private HttpRequest postRequest(String url, String body, String token, long timeoutMs) {
    return setupARequest(url, token, timeoutMs)
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
  }

  protected void doDelete(String url, String body) throws IOException {
    LOGGER.debug("doDelete: " + url + " body: " + body);
    HttpRequest request = deleteRequest(url, body, token, DEFAULT_TIMEOUT_MS);
    doRequest(request);
  }

  private HttpRequest deleteRequest(String url, String body, String token, long timeoutMs) {
    return setupARequest(url, token, timeoutMs)
        .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
        .build();
  }

  private String doRequest(HttpRequest request) throws IOException {
    LOGGER.debug("method: " + request.method() + " request.uri: " + request.uri());
    String result = "";
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      LOGGER.debug("method: " + request.method() + " response: " + response);
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
}
