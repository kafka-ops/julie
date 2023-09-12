package com.purbon.kafka.topology.model.artefact;

import java.util.Map;

@TypeArtefact(name = "VARS")
public class KsqlVarsArtefact extends KsqlArtefact {
  private Map<String, String> sessionVars;
  private static final String KSQLDB_VARS_NAME = "SESSION_VARS";

  public KsqlVarsArtefact(Map<String, String> sessionVars) {
    super("", "", KSQLDB_VARS_NAME);
    this.sessionVars = sessionVars;
  }

  public Map<String, String> getSessionVars() {
    return this.sessionVars;
  }

  public void setSessionVars(Map<String, String> sessionVars) {
    this.sessionVars = sessionVars;
  }
}
