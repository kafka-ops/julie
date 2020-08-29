package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SetAclsForKConnect extends BaseAccessControlAction {

  private final Connector app;
  private final String topicPrefix;
  private final AccessControlProvider controlProvider;

  public SetAclsForKConnect(
      AccessControlProvider controlProvider, Connector app, String topicPrefix) {
    super();
    this.app = app;
    this.topicPrefix = topicPrefix;
    this.controlProvider = controlProvider;
  }

  @Override
  public void run() throws IOException {
    bindings = controlProvider.setAclsForConnect(app, topicPrefix);
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", app.getPrincipal());
    map.put("Topic", topicPrefix);

    try {
      return JSON.asString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
