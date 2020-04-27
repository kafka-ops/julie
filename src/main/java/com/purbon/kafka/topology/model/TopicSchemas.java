package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.LowerDotCaseStrategy.class)
public class TopicSchemas {

  private String keySchemaString;
  private String keySchemaType;
  // TODO private String keySchemaFile;

  private String valueSchemaString;
  private String valueSchemaType;
  // TODO private String valueSchemaFile;

  public TopicSchemas() {}

  public String getKeySchemaString() {
    return keySchemaString;
  }

  public void setKeySchemaString(String keySchemaString) {
    this.keySchemaString = keySchemaString;
  }

  public String getKeySchemaType() {
    return keySchemaType;
  }

  public void setKeySchemaType(String keySchemaType) {
    this.keySchemaType = keySchemaType;
  }

  public String getValueSchemaString() {
    return valueSchemaString;
  }

  public void setValueSchemaString(String valueSchemaString) {
    this.valueSchemaString = valueSchemaString;
  }

  public String getValueSchemaType() {
    return valueSchemaType;
  }

  public void setValueSchemaType(String valueSchemaType) {
    this.valueSchemaType = valueSchemaType;
  }
}
