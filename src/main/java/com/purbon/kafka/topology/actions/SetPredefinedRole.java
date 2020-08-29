package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SetPredefinedRole extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final String principal;
  private final String predefinedRole;
  private final String topicPrefix;

  public SetPredefinedRole(
      AccessControlProvider controlProvider,
      String principal,
      String predefinedRole,
      String topicPrefix) {
    super();
    this.controlProvider = controlProvider;
    this.principal = principal;
    this.predefinedRole = predefinedRole;
    this.topicPrefix = topicPrefix;
  }

  @Override
  public void run() throws IOException {
    TopologyAclBinding binding =
        controlProvider.setPredefinedRole(principal, predefinedRole, topicPrefix);
    bindings = Collections.singletonList(binding);
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", principal);
    map.put("Role", predefinedRole);
    map.put("Topic", topicPrefix);

    try {
      return JSON.asString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
