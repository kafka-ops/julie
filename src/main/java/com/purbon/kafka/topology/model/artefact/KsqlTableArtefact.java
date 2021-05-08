package com.purbon.kafka.topology.model.artefact;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KsqlTableArtefact extends KsqlArtefact {

  @JsonCreator
  public KsqlTableArtefact(
      @JsonProperty("path") String path,
      @JsonProperty("server") String label,
      @JsonProperty("name") String name) {
    super(path, label, name);
  }
}
