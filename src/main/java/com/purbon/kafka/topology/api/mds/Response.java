package com.purbon.kafka.topology.api.mds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Response {

  private static final Logger LOGGER = LogManager.getLogger(Response.class);
  private static final String STATUS_FIELD = "status";

  private final String response;
  private final Map<String, Object> map;
  private final int statusCode;
  private final Header headers;

  public Response(CloseableHttpResponse httpResponse) {
    HttpEntity entity = httpResponse.getEntity();
    this.headers = entity.getContentType();
    this.statusCode = httpResponse.getStatusLine().getStatusCode();
    this.response = parseBodyAsString(entity);
    this.map = responseToJson(response);
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

  private String parseBodyAsString(HttpEntity entity) {
    String result = "";
    if (entity != null) {
      try {
        result = EntityUtils.toString(entity);
      } catch (IOException e) {
        LOGGER.error(e);
      }
    }
    return result;
  }

  public String getResponseAsString() {
    return response;
  }
}
