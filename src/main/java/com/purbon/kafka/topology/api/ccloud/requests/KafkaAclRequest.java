package com.purbon.kafka.topology.api.ccloud.requests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KafkaAclRequest implements CCloudRequest {

  private static final Logger LOGGER = LogManager.getLogger(KafkaAclRequest.class);

  private TopologyAclBinding binding;
  private String url;

  public KafkaAclRequest(TopologyAclBinding binding, String url) {
    this.binding = binding;
    this.url = url;
  }

  public String deleteUrl() {
    var props = asMap();
    props.remove("kind");
    return url + "?" + urlParams(props);
  }

  private Map<String, Object> asMap() {
    Map<String, Object> request = new HashMap<>();
    request.put("resource_type", binding.getResourceType());
    request.put("resource_name", binding.getResourceName());
    request.put("pattern_type", binding.getPattern());
    request.put("principal", binding.getPrincipal());
    request.put("host", binding.getHost());
    request.put("operation", binding.getOperation());
    request.put("permission", "ALLOW");
    return request;
  }

  private String urlParams(Map<String, Object> props) {
    return props.entrySet().stream()
        .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
        .collect(Collectors.joining("&"));
  }

  @Override
  public String asJson() {
    var request = asMap();

    try {
      return JSON.asString(request);
    } catch (JsonProcessingException e) {
      LOGGER.warn(e);
      return "";
    }
  }
}
