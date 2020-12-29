package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Optional;

@JsonNaming(PropertyNamingStrategy.LowerDotCaseStrategy.class)
public class TopicSchemas {

  private Optional<String> keySchemaFile;
  private Optional<String> keyRecordType;
  private Optional<String> valueSchemaFile;
  private Optional<String> valueRecordType;

  public TopicSchemas() {
    this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public TopicSchemas(
      Optional<JsonNode> keyJsonNode,
      Optional<JsonNode> keyRecordJsonNode,
      Optional<JsonNode> valueJsonNode,
      Optional<JsonNode> valueRecordJsonNode) {
    this.keySchemaFile = keyJsonNode.map(JsonNode::asText);
    this.keyRecordType = keyRecordJsonNode.map(JsonNode::asText);
    this.valueSchemaFile = valueJsonNode.map(JsonNode::asText);
    this.valueRecordType = valueRecordJsonNode.map(JsonNode::asText);
  }

  public TopicSchemas(String keySchemaFile, String valueSchemaFile) {
    this.keySchemaFile = Optional.of(keySchemaFile);
    this.valueSchemaFile = Optional.of(valueSchemaFile);
    this.keyRecordType = Optional.empty();
    this.valueRecordType = Optional.empty();
  }

  public Optional<String> getKeySchemaFile() {
    return keySchemaFile;
  }

  public Optional<String> getValueSchemaFile() {
    return valueSchemaFile;
  }

  public Optional<String> getKeyRecordType() {
    return keyRecordType;
  }

  public Optional<String> getValueRecordType() {
    return valueRecordType;
  }
}
