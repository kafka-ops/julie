package com.purbon.kafka.topology.model.artefact;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.purbon.kafka.topology.model.Artefact;
import org.jetbrains.annotations.NotNull;

public class KsqlArtefact extends Artefact implements Comparable<KsqlArtefact> {

  @JsonCreator
  public KsqlArtefact(
      @JsonProperty("path") String path,
      @JsonProperty("server") String label,
      @JsonProperty("name") String name) {
    super(path, label, name);
  }

  @Override
  public int compareTo(@NotNull KsqlArtefact o) {
    // Streams get precedence over Tables, if within the same type they are equal so order
    // during deserialization will be based on the position in the yaml
    if ((this instanceof KsqlStreamArtefact) && (o instanceof KsqlTableArtefact)) {
      return -1;
    } else if ((this instanceof KsqlTableArtefact) && (o instanceof KsqlStreamArtefact)) {
      return 1;
    } else {
      return 0;
    }
  }
}
