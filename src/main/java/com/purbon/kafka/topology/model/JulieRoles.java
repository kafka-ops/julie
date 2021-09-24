package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.purbon.kafka.topology.model.users.Other;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JulieRoles {

  private Map<String, JulieRole> roles;

  public JulieRoles() {
    this.roles = Collections.emptyMap();
  }

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

  public void validateTopology(Topology topology) throws IOException {
    if (roles.isEmpty()) {
      return;
    }
    for (Project project : topology.getProjects()) {
      for (Map.Entry<String, List<Other>> other : project.getOthers().entrySet()) {
        if (!roles.containsKey(other.getKey())) {
          throw new IOException(
              "trying to deploy role: "
                  + other.getKey()
                  + " not available in the configured roles: "
                  + roles.keySet().stream().collect(Collectors.joining(", ")));
        }
      }
    }
  }

  public int size() {
    return roles.size();
  }
}
