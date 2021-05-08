package com.purbon.kafka.topology.model.artefact;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KsqlStreamArtefact extends KsqlArtefact {

  @JsonCreator
  public KsqlStreamArtefact(
      @JsonProperty("path") String path,
      @JsonProperty("server") String label,
      @JsonProperty("name") String name) {
    super(path, label, name);
  }
}
