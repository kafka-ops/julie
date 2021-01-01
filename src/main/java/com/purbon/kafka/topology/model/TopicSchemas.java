package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.purbon.kafka.topology.model.schema.Subject;
import java.util.Optional;

@JsonNaming(PropertyNamingStrategy.LowerDotCaseStrategy.class)
public class TopicSchemas {

  private Subject keySubject;
  private Subject valueSubject;

  public TopicSchemas(
      Optional<JsonNode> keyJsonNode,
      Optional<JsonNode> keyRecordJsonNode,
      Optional<JsonNode> valueJsonNode,
      Optional<JsonNode> valueRecordJsonNode) {
    this.keySubject = new Subject(keyJsonNode, keyRecordJsonNode);
    this.valueSubject = new Subject(valueJsonNode, valueRecordJsonNode);
  }

  public TopicSchemas(String keySchemaFile, String valueSchemaFile) {
    this.keySubject = new Subject(keySchemaFile, null);
    this.valueSubject = new Subject(valueSchemaFile, null);
  }

  public Subject getKeySubject() {
    return keySubject;
  }

  public Subject getValueSubject() {
    return valueSubject;
  }
}
