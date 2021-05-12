package com.purbon.kafka.topology.model.artefact;

import com.purbon.kafka.topology.model.Artefacts;
import java.util.ArrayList;
import java.util.List;

public class KsqlArtefacts implements Artefacts {

  private List<KsqlStreamArtefact> streams;
  private List<KsqlTableArtefact> tables;

  public KsqlArtefacts() {
    this(new ArrayList<>(), new ArrayList<>());
  }

  public KsqlArtefacts(List<KsqlStreamArtefact> streams, List<KsqlTableArtefact> tables) {
    this.streams = streams;
    this.tables = tables;
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
