package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.model.users.KSqlApp;

public class KSqlAppAclBindingsBuilder implements AclBindingsBuilder {

  private final BindingsBuilderProvider builderProvider;
  private final KSqlApp app;
  private final String prefix;

  public KSqlAppAclBindingsBuilder(
      BindingsBuilderProvider builderProvider, KSqlApp app, String topicPrefix) {
    this.builderProvider = builderProvider;
    this.app = app;
    this.prefix = app.getApplicationId().orElse(topicPrefix);
  }

  @Override
  public AclBindingsResult getAclBindings() {
    if (prefix.isEmpty()) {
      return AclBindingsResult.forError(
          "KSqlApp application prefix should not be empty."
              + " Please define the applicationID or allow a nonEmpty project prefix (aka everything before the topic)");
    }
    return AclBindingsResult.forAclBindings(builderProvider.buildBindingsForKSqlApp(app, prefix));
  }
}
