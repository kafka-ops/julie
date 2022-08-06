package com.purbon.kafka.topology.api.ccloud.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KafkaAclRequest {

  @JsonIgnore private String url;

  private String resource_type;
  private String resource_name;
  private String pattern_type;
  private String principal;
  private String host;
  private String operation;
  private String permission;

  public KafkaAclRequest(TopologyAclBinding binding, String url) {
    this.url = url;
    this.resource_name = binding.getResourceName();
    this.resource_type = binding.getResourceType();
    this.pattern_type = binding.getPattern();
    this.principal = binding.getPrincipal();
    this.host = binding.getHost();
    this.operation = binding.getOperation();
    this.permission = "ALLOW";
  }

  public String deleteUrl() {
    return url + "?" + urlParams(asMap());
  }

  private Map<String, Object> asMap() {
    Map<String, Object> request = new HashMap<>();
    request.put("resource_type", resource_type);
    request.put("resource_name", resource_name);
    request.put("pattern_type", pattern_type);
    request.put("principal", principal);
    request.put("host", host);
    request.put("operation", operation);
    request.put("permission", permission);
    return request;
  }

  private String urlParams(Map<String, Object> props) {
    return props.entrySet().stream()
        .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
        .collect(Collectors.joining("&"));
  }
}
