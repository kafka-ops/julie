package com.purbon.kafka.topology.model.artefact;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.purbon.kafka.topology.model.Artefact;

public class KsqlArtefact extends Artefact {

  @JsonCreator
  public KsqlArtefact(
      @JsonProperty("path") String path,
      @JsonProperty("server") String label,
      @JsonProperty("name") String name) {
    super(path, label, name);
  }
}
