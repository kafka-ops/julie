package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.model.users.platform.KsqlServerInstance;

public class KSqlServerAclBindingsBuilder implements AclBindingsBuilder {

  private final BindingsBuilderProvider builderProvider;
  private final KsqlServerInstance ksqlServer;

  public KSqlServerAclBindingsBuilder(
      BindingsBuilderProvider builderProvider, KsqlServerInstance ksqlServer) {
    this.builderProvider = builderProvider;
    this.ksqlServer = ksqlServer;
  }

  @Override
  public AclBindingsResult getAclBindings() {
    return AclBindingsResult.forAclBindings(builderProvider.buildBindingsForKSqlServer(ksqlServer));
  }
}
