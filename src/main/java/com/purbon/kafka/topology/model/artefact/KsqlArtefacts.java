package com.purbon.kafka.topology.model.artefact;

import com.purbon.kafka.topology.model.Artefacts;
import java.util.*;

public class KsqlArtefacts implements Artefacts {

  private List<KsqlStreamArtefact> streams;
  private List<KsqlTableArtefact> tables;
  private final KsqlVarsArtefact vars;

  public KsqlArtefacts() {
    this(new ArrayList<>(), new ArrayList<>(), new KsqlVarsArtefact(Collections.emptyMap()));
  }

  public KsqlArtefacts(
      List<KsqlStreamArtefact> streams, List<KsqlTableArtefact> tables, KsqlVarsArtefact vars) {
    this.streams = streams;
    this.tables = tables;
    this.vars = vars;
  }

  public KsqlVarsArtefact getVars() {
    return this.vars;
  }

  public List<KsqlStreamArtefact> getStreams() {
    return streams;
  }

  public void setStreams(List<KsqlStreamArtefact> streams) {
    this.streams = streams;
  }

  public List<KsqlTableArtefact> getTables() {
    return tables;
  }

  public void setTables(List<KsqlTableArtefact> tables) {
    this.tables = tables;
  }
}
