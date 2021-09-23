package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JulieRoles {

  private Map<String, JulieRole> roles;

  @JsonCreator
  public JulieRoles(@JsonProperty("roles") List<JulieRole> roles) {
    this.roles = roles.stream().collect(Collectors.toMap(JulieRole::getName, e -> e));
  }

  public List<JulieRole> getRoles() {
    return new ArrayList<>(roles.values());
  }

  public JulieRole get(String key) {
    return roles.get(key);
  }
}
