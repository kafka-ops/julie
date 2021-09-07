package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.model.users.KStream;

public class KStreamsAclBindingsBuilder implements AclBindingsBuilder {

  private final BindingsBuilderProvider builderProvider;
  private final KStream app;
  private final String prefix;

  public KStreamsAclBindingsBuilder(
      BindingsBuilderProvider builderProvider, KStream app, String topicPrefix) {
    this.builderProvider = builderProvider;
    this.app = app;
    this.prefix = app.getApplicationId().orElse(topicPrefix);
  }

  @Override
  public AclBindingsResult getAclBindings() {
    if (prefix.isEmpty()) {
      return AclBindingsResult.forError(
          "KStream application prefix should not be empty."
              + " Please define the applicationID or allow a nonEmpty project prefix (aka everything before the topic)");
    }
    return AclBindingsResult.forAclBindings(
        builderProvider.buildBindingsForStreamsApp(
            app.getPrincipal(),
            prefix,
            app.getTopics().get(KStream.READ_TOPICS),
            app.getTopics().get(KStream.WRITE_TOPICS)));
  }
}
