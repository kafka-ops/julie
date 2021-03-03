package com.purbon.kafka.topology.api.mds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.utils.JSON;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Response {

  private static final Logger LOGGER = LogManager.getLogger(Response.class);
  private static final String STATUS_FIELD = "status";

  private final String response;
  private final Map<String, Object> map;
  private final int statusCode;
  private final HttpHeaders headers;

  public Response(HttpResponse<String> response) {
    this.headers = response.headers();
    this.statusCode = response.statusCode();
    this.response = response.body();
    this.map = responseToJson(this.response);
  }

  public Integer getStatus() {
    return statusCode;
  }

  public Object getField(String field) {
    return map.get(field);
  }

  private Map<String, Object> responseToJson(String response) {
    try {
      return JSON.toMap(response);
    } catch (JsonProcessingException e) {
      LOGGER.error(e);
      return new HashMap<>();
    }
  }

  public String getResponseAsString() {
    return response;
  }
}
