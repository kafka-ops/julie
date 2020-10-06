package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.LowerDotCaseStrategy.class)
public class TopicSchemas {

  private String keySchemaFile;
  private String valueSchemaFile;

  public TopicSchemas(String keySchemaFile, String valueSchemaFile) {
    this.keySchemaFile = keySchemaFile;
    this.valueSchemaFile = valueSchemaFile;
  }

  public String getKeySchemaFile() {
    return keySchemaFile;
  }

  public String getValueSchemaFile() {
    return valueSchemaFile;
  }
}
