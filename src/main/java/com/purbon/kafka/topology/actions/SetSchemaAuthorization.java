package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Schemas;
import java.io.IOException;

public class SetSchemaAuthorization extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final Schemas schemaAuthorization;

  public SetSchemaAuthorization(
      AccessControlProvider controlProvider, Schemas schemaAuthorization) {
    super();
    this.controlProvider = controlProvider;
    this.schemaAuthorization = schemaAuthorization;
  }

  @Override
  public void run() throws IOException {

    bindings =
        controlProvider.setSchemaAuthorization(
            schemaAuthorization.getPrincipal(), schemaAuthorization.getSubjects());
  }
}
