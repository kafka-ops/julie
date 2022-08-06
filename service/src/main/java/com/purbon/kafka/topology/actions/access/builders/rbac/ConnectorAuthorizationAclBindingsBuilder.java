package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.access.builders.AclBindingsBuilder;
import com.purbon.kafka.topology.actions.access.builders.AclBindingsResult;
import com.purbon.kafka.topology.model.users.Connector;
import java.util.ArrayList;

public class ConnectorAuthorizationAclBindingsBuilder implements AclBindingsBuilder {

  private final Connector connector;
  private final BindingsBuilderProvider builderProvider;

  public ConnectorAuthorizationAclBindingsBuilder(
      BindingsBuilderProvider builderProvider, Connector connector) {
    this.builderProvider = builderProvider;
    this.connector = connector;
  }

  @Override
  public AclBindingsResult getAclBindings() {
    return AclBindingsResult.forAclBindings(
        builderProvider.setConnectorAuthorization(
            connector.getPrincipal(), connector.getConnectors().orElse(new ArrayList<>())));
  }
}
