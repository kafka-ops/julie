package com.purbon.kafka.topology.model.schemas;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import java.util.Optional;

public class Subject {

  private Optional<String> optionalFile;
  private Optional<String> optionalCompatibility;
  private Optional<String> optionalFormat;

  public Subject(Optional<String> optionalFile, Optional<String> optionalFormat) {
    this(optionalFile, optionalFormat, Optional.empty());
  }

  public Subject(
      Optional<String> optionalFile,
      Optional<String> optionalFormat,
      Optional<String> optionalCompatibility) {
    this.optionalFile = optionalFile;
    this.optionalFormat = optionalFormat;
    this.optionalCompatibility = optionalCompatibility;
  }

  public Optional<String> getOptionalFormat() {
    return optionalFormat;
  }

  public String getFormat() {
    return optionalFormat.orElse(AvroSchema.TYPE);
  }

  public Optional<String> getOptionalFile() {
    return optionalFile;
  }

  public Optional<String> getOptionalCompatibility() {
    return optionalCompatibility;
  }

  public void setOptionalCompatibility(Optional<String> optionalCompatibility) {
    this.optionalCompatibility = optionalCompatibility;
  }
}
