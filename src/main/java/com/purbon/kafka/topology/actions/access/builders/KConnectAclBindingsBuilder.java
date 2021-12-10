package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.model.users.Connector;

public class KConnectAclBindingsBuilder implements AclBindingsBuilder {

  private final Connector app;
  private final String topicPrefix;
  private final BindingsBuilderProvider controlProvider;

  public KConnectAclBindingsBuilder(
      BindingsBuilderProvider controlProvider, Connector app, String topicPrefix) {
    this.app = app;
    this.topicPrefix = topicPrefix;
    this.controlProvider = controlProvider;
  }

  @Override
  public AclBindingsResult getAclBindings() {
    return AclBindingsResult.forAclBindings(
        controlProvider.buildBindingsForConnect(app, topicPrefix));
  }
}
