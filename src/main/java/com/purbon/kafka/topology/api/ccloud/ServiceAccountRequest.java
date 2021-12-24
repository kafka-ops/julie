package com.purbon.kafka.topology.api.ccloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.utils.JSON;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceAccountRequest implements CCloudRequest {

  private static final Logger LOGGER = LogManager.getLogger(ServiceAccountRequest.class);

  private String name;
  private String description;

  public ServiceAccountRequest(String name) {
    this.name = name;
    this.description = "Managed by JulieOps";
  }

  @Override
  public String asJson() {
    Map<String, Object> request = new HashMap<>();
    request.put("display_name", name);
    request.put("description", description);

    try {
      return JSON.asString(request);
    } catch (JsonProcessingException e) {
      LOGGER.warn(e);
      return "";
    }
  }
}
