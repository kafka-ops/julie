package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.LowerDotCaseStrategy.class)
public class TopicSchemas {

  private String keySchemaFile;
  private String valueSchemaFile;

  public TopicSchemas() {}

  public String getKeySchemaFile() {
    return keySchemaFile;
  }

  public void setKeySchemaFile(String keySchemaFile) {
    this.keySchemaFile = keySchemaFile;
  }

  public String getValueSchemaFile() {
    return valueSchemaFile;
  }

  public void setValueSchemaFile(String valueSchemaFile) {
    this.valueSchemaFile = valueSchemaFile;
  }
}
