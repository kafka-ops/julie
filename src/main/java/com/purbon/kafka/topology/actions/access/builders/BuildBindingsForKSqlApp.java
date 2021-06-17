package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.KSqlApp;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BuildBindingsForKSqlApp extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final KSqlApp app;
  private final String prefix;

  public BuildBindingsForKSqlApp(
      BindingsBuilderProvider builderProvider, KSqlApp app, String topicPrefix) {
    super();
    this.builderProvider = builderProvider;
    this.app = app;
    this.prefix = app.getApplicationId().orElse(topicPrefix);
  }

  @Override
  protected void execute() throws IOException {
    if (prefix.isEmpty()) {
      String message =
          "KSqlApp application prefix should not be empty."
              + " Please define the applicationID or allow a nonEmpty project prefix (aka everything before the topic";
      throw new IOException(message);
    }

    bindings = builderProvider.buildBindingsForKSqlApp(app, prefix);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", app.getPrincipal());
    map.put("Topic", prefix);
    return map;
  }
}
